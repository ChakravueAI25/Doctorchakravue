import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:intl/intl.dart';
import 'call_screen.dart';
import 'patient_record_screen.dart';
 // import the file you just made

class SubmissionDetailScreen extends StatefulWidget {
  final Map<String, dynamic> submission;
  final String baseUrl;
  final String doctorId;

  const SubmissionDetailScreen({super.key, required this.submission, required this.baseUrl, required this.doctorId});

  @override
  State<SubmissionDetailScreen> createState() => _SubmissionDetailScreenState();
}

class _SubmissionDetailScreenState extends State<SubmissionDetailScreen> {
  final _noteController = TextEditingController();
  bool _isSending = false;

  Map<String, dynamic>? _patientRecord;
  bool _isPatientLoading = false;

  @override
  void initState() {
    super.initState();
    _loadPatientRecord();
  }

  Future<void> _loadPatientRecord() async {
    setState(() => _isPatientLoading = true);
    try {
      final submission = widget.submission;
      final String? email = (submission['patient_email'] ?? submission['email'])?.toString();
      final String? pid = (submission['patient_id'] ?? submission['patient'] ?? submission['patient_oid'])?.toString();
      Map<String, dynamic>? record;

      // Prefer case/search (matches patientDetails/contactInfo)
      if (email != null && email.isNotEmpty) {
        final resp = await http.get(Uri.parse("${widget.baseUrl}/patients/case/search/?query=${Uri.encodeQueryComponent(email)}"));
        if (resp.statusCode == 200) record = json.decode(resp.body) as Map<String, dynamic>;
      }

      // If not found, try case endpoint by pid (backend supports /patients/case/{pid})
      if (record == null && pid != null && pid.isNotEmpty) {
        final resp = await http.get(Uri.parse("${widget.baseUrl}/patients/case/${Uri.encodeComponent(pid)}"));
        if (resp.statusCode == 200) record = json.decode(resp.body) as Map<String, dynamic>;
      }

      // final fallback: name search via case/search
      if (record == null) {
        final name = submission['patient_name']?.toString() ?? '';
        if (name.isNotEmpty) {
          final resp = await http.get(Uri.parse("${widget.baseUrl}/patients/case/search/?query=${Uri.encodeQueryComponent(name)}"));
          if (resp.statusCode == 200) record = json.decode(resp.body) as Map<String, dynamic>;
        }
      }

      if (mounted) setState(() => _patientRecord = record);
    } catch (_) {
      // ignore
    } finally {
      if (mounted) setState(() => _isPatientLoading = false);
    }
  }

  String? _extractId(dynamic v) {
    if (v == null) return null;
    if (v is String && v.isNotEmpty) return v;
    if (v is Map) {
      if (v.containsKey(r'$oid') && v[r'$oid'] is String) return v[r'$oid'];
      if (v.containsKey('\$oid') && v['\$oid'] is String) return v['\$oid'];
      if (v.containsKey('oid') && v['oid'] is String) return v['oid'];
    }
    final s = v.toString();
    return s.isNotEmpty ? s : null;
  }

  Future<void> _sendNote() async {
    final note = _noteController.text.trim();
    if (note.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Enter a note to send")));
      return;
    }

    final submission = widget.submission;
    final String? email = (submission['patient_email'] ?? submission['email'])?.toString();
    final bool hasEmail = email != null && email.isNotEmpty;
    if (!hasEmail) {
      // warn but continue: note should still be attached to the submission so dashboard can receive it
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Patient email not available — sending note to submission only")));
    }

    setState(() => _isSending = true);
    bool sentOk = false;

    if (hasEmail) {
      try {
        final uri = Uri.parse("${widget.baseUrl}/patients/${Uri.encodeComponent(email!)}/messages");
        final resp = await http.post(
          uri,
          headers: {"Content-Type": "application/json"},
          body: json.encode({"field": "note", "message": note}),
        );
        if (resp.statusCode == 200 || resp.statusCode == 201) {
          sentOk = true;
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Note sent to patient")));
        } else {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Failed to send note (${resp.statusCode})")));
        }
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Network error sending note")));
      }
    }

    // Also notify the submission-notes endpoint so the web dashboard receives the note
    try {
      final sid = _extractId(submission['_id'] ?? submission['id'] ?? submission['submission_id']);
      if (sid != null) {
        final msgUri = Uri.parse("${widget.baseUrl}/submissions/${Uri.encodeComponent(sid)}/notes");
        await http.post(
          msgUri,
          headers: {"Content-Type": "application/json"},
          // send NoteCreate-like payload: keep it simple and include note, author and timestamp
          body: json.encode({
            'note': note,
            'author': 'doctor',
            'timestamp': DateTime.now().toUtc().toIso8601String(),
          }),
        );
      }
    } catch (_) {
      // ignore failures here; main patient message is primary
    }

    try {
      final sid = _extractId(submission['_id'] ?? submission['id'] ?? submission['submission_id']);
      if (sid != null) {
        final archiveUri = Uri.parse("${widget.baseUrl}/submissions/${Uri.encodeComponent(sid)}/archive");
        await http.post(archiveUri);
      }
    } catch (_) {}

    setState(() => _isSending = false);

    if (sentOk) {
      _noteController.clear();
      Navigator.of(context).pop();
    }
  }

  Future<void> _startVideoCall() async {
    setState(() => _isSending = true);

    final sub = widget.submission;
    final String? patientId = (sub['patient_id'] ?? sub['patient'] ?? sub['patient_oid'] ?? sub['id'] ?? sub['_id'])?.toString();

    if (patientId == null || patientId.isEmpty || patientId == 'null') {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Error: Cannot find Patient ID for this submission")));
      setState(() => _isSending = false);
      return;
    }

    final channelName = "call_$patientId";

    try {
      final tokenUrl = Uri.parse("${widget.baseUrl}/call/token?channel_name=$channelName");
      final tokenResp = await http.post(tokenUrl);
      if (tokenResp.statusCode != 200) throw "Token Error: ${tokenResp.body}";
      final tokenData = json.decode(tokenResp.body);
      final doctorToken = tokenData['token'];
      final appId = tokenData['app_id'];

      final notifyUrl = Uri.parse("${widget.baseUrl}/call/initiate");
      final notifyResp = await http.post(
        notifyUrl,
        headers: {"Content-Type": "application/json"},
        body: json.encode({
          "doctor_id": widget.doctorId,
          "patient_id": patientId,
          "channel_name": channelName,
        }),
      );

      if (notifyResp.statusCode != 200) {
        throw "Call Init Error: ${notifyResp.statusCode} ${notifyResp.body}";
      }

      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => CallScreen(
            appId: appId,
            token: doctorToken,
            channelName: channelName,
          ),
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Call failed: $e")));
    } finally {
      setState(() => _isSending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final String imageUrl = "${widget.baseUrl}/files/${widget.submission['image_file_id']}";
    final String patientName = widget.submission['patient_name'] ?? 'Patient';
    final int painScale = widget.submission['pain_scale'] ?? 0;
    final int swelling = widget.submission['swelling'] ?? 0;
    final int redness = widget.submission['redness'] ?? 0;
    final int discharge = widget.submission['discharge'] ?? 0;
    final String comments = (widget.submission['comments'] ?? "").toString();

    String formattedTimestamp = 'No date';
    if (widget.submission['timestamp'] != null) {
      try {
        final DateTime parsedDate = DateTime.parse(widget.submission['timestamp']).toLocal();
        formattedTimestamp = DateFormat('dd-MM-yyyy hh:mm a').format(parsedDate);
      } catch (_) {}
    }

    return Scaffold(
      appBar: AppBar(title: Text("$patientName's Submission")),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Submitted on: $formattedTimestamp', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Reported Pain Scale:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
                      const SizedBox(height: 6),
                      Text('$painScale / 10', style: Theme.of(context).textTheme.headlineSmall?.copyWith(color: Colors.red, fontWeight: FontWeight.bold)),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(color: Colors.red.withOpacity(0.08), borderRadius: BorderRadius.circular(8)),
                  child: Column(children: [const Icon(Icons.warning_amber_rounded, color: Colors.red), const SizedBox(height: 4), Text("$painScale/10", style: const TextStyle(color: Colors.red))]),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 10,
              runSpacing: 8,
              children: [
                Chip(avatar: const Icon(Icons.add_circle_outline, size: 18, color: Colors.white70), label: Text('Swelling: $swelling', style: const TextStyle(color: Colors.white))),
                Chip(avatar: const Icon(Icons.brightness_1, size: 12, color: Colors.white70), label: Text('Redness: $redness', style: const TextStyle(color: Colors.white))),
                Chip(avatar: const Icon(Icons.opacity, size: 18, color: Colors.white70), label: Text('Watering: $discharge', style: const TextStyle(color: Colors.white))),
              ],
            ),
            const SizedBox(height: 18),
            if (comments.isNotEmpty) ...[
              const Text('Patient Comments', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
              const SizedBox(height: 8),
              Card(color: const Color(0xFF0F1416), child: Padding(padding: const EdgeInsets.all(12.0), child: Text(comments, style: const TextStyle(color: Colors.white70)))),
              const SizedBox(height: 18),
            ],
            const Text('Submitted Image:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
            const SizedBox(height: 12),
            Center(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(
                  imageUrl,
                  fit: BoxFit.cover,
                  width: double.infinity,
                  height: 320,
                  loadingBuilder: (context, child, loadingProgress) {
                    if (loadingProgress == null) return child;
                    return const Center(child: Padding(padding: EdgeInsets.all(32.0), child: CircularProgressIndicator()));
                  },
                  errorBuilder: (context, error, stackTrace) {
                    return const Center(child: Padding(padding: EdgeInsets.all(32.0), child: Icon(Icons.broken_image, size: 60, color: Colors.white38)));
                  },
                ),
              ),
            ),
            const SizedBox(height: 24),
            if (_isPatientLoading)
              const Center(child: Padding(padding: EdgeInsets.symmetric(vertical: 12), child: CircularProgressIndicator()))
            else ...[
              if (_patientRecord == null)
                Padding(padding: const EdgeInsets.symmetric(vertical: 8.0), child: Text("No patient record found. Showing submission fields only.", style: Theme.of(context).textTheme.bodyMedium)),
              _infoCard("Presenting Complaints", _buildComplaints(_patientRecord ?? {})),
              _infoCard("Medicines / Current meds", _buildMedicines(_patientRecord ?? {})),
              _infoCard("Prescription / Doctor notes", _buildPrescription(_patientRecord ?? {})),
              _infoCard("Investigations", _buildInvestigations(_patientRecord ?? {})),
              if (_patientRecord != null)
                Align(alignment: Alignment.centerRight, child: OutlinedButton.icon(icon: const Icon(Icons.open_in_new), label: const Text("View Full Record"), onPressed: () { Navigator.of(context).push(MaterialPageRoute(builder: (_) => PatientRecordScreen(record: _patientRecord!))); })),
            ],
            const Divider(thickness: 1, color: Colors.white12),
            const SizedBox(height: 12),
            const Text('Send Note to Patient:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
            const SizedBox(height: 8),
            TextField(controller: _noteController, decoration: const InputDecoration(border: OutlineInputBorder(), hintText: "e.g., Please apply the prescribed ointment...", labelText: "Your Note"), maxLines: 4),
            const SizedBox(height: 12),
            ElevatedButton.icon(onPressed: _isSending ? null : _sendNote, icon: _isSending ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 3, color: Colors.black)) : const Icon(Icons.send), label: Text(_isSending ? "Sending..." : "Send Note & Archive"), style: ElevatedButton.styleFrom(minimumSize: const Size(double.infinity, 50))),
            const SizedBox(height: 8),
            ElevatedButton.icon(
              onPressed: _isSending ? null : _startVideoCall,
              icon: const Icon(Icons.video_call),
              label: const Text("Video Call Patient"),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
            ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }

  Widget _infoCard(String title, Widget child) {
    return Card(
      color: Theme.of(context).cardTheme.color,
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: Padding(padding: const EdgeInsets.all(12), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(fontWeight: FontWeight.w700, color: Colors.white)), const SizedBox(height: 8), child])),
    );
  }

  Widget _buildComplaints(Map<String, dynamic> p) {
    final complaints = (p['presentingComplaints']?['complaints'] as List?) ?? [];
    if (complaints.isEmpty) return const Text("No presenting complaints", style: TextStyle(color: Colors.white70));
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: complaints.map((c) { final m = c is Map ? c : {'complaint': c.toString()}; return Padding(padding: const EdgeInsets.only(bottom: 6), child: Text("- ${m['complaint'] ?? ''}${(m['duration'] != null && (m['duration'] as String).isNotEmpty) ? ' • ${m['duration']}' : ''}", style: const TextStyle(color: Colors.white70))); }).toList());
  }

  Widget _buildMedicines(Map<String, dynamic> p) { final meds = (p['drugHistory']?['currentMeds'] as List?) ?? (p['medications'] as List?) ?? []; if (meds.isEmpty) return const Text("No current medicines", style: TextStyle(color: Colors.white70)); return Column(crossAxisAlignment: CrossAxisAlignment.start, children: meds.map((m) { final mm = m is Map ? m : {'name': m.toString()}; final name = mm['name'] ?? mm['drug'] ?? ''; final dose = mm['dosage'] ?? ''; final ind = mm['indication'] ?? ''; return Padding(padding: const EdgeInsets.only(bottom: 6), child: Text("- $name${dose != '' ? ' • $dose' : ''}${ind != '' ? ' • $ind' : ''}", style: const TextStyle(color: Colors.white70))); }).toList()); }

  Widget _buildPrescription(Map<String, dynamic> p) {
    final pres = p['doctor']?['prescription'] ?? p['medications'] ?? {};
    if (pres is Map && pres.isEmpty) return const Text("No prescription recorded", style: TextStyle(color: Colors.white70));
    if (pres is List && pres.isEmpty) return const Text("No prescription recorded", style: TextStyle(color: Colors.white70));
    if (pres is Map) return Column(crossAxisAlignment: CrossAxisAlignment.start, children: pres.entries.map((e) => Padding(padding: const EdgeInsets.only(bottom: 6), child: Text("- ${e.key}: ${e.value}", style: const TextStyle(color: Colors.white70)))).toList());
    if (pres is List) return Column(crossAxisAlignment: CrossAxisAlignment.start, children: pres.map<Widget>((item) => Padding(padding: const EdgeInsets.only(bottom: 6), child: Text("- ${item.toString()}", style: const TextStyle(color: Colors.white70)))).toList());
    return Text(pres.toString(), style: const TextStyle(color: Colors.white70));
  }

  Widget _buildInvestigations(Map<String, dynamic> p) {
    final inv = p['investigations'] ?? {};
    if (inv is! Map || inv.isEmpty) return const Text("No investigations", style: TextStyle(color: Colors.white70));
    final List<Widget> rows = [];
    if ((inv['iop']?['iopReadings'] as List?)?.isNotEmpty ?? false) {
      final iops = inv['iop']['iopReadings'] as List;
      rows.add(Text("IOP readings:", style: const TextStyle(color: Colors.white70)));
      for (final r in iops) { rows.add(Padding(padding: const EdgeInsets.only(left: 8.0, bottom: 6), child: Text("- ${r['type'] ?? ''} ${r['time'] ?? ''} • OD:${r['od'] ?? ''} OS:${r['os'] ?? ''}", style: const TextStyle(color: Colors.white70)))); }
    }
    if ((inv['ophthalmicInvestigations'] as Map?)?.isNotEmpty ?? false) {
      rows.add(const SizedBox(height: 6));
      rows.add(Text("Ophthalmic:", style: const TextStyle(color: Colors.white70)));
      inv['ophthalmicInvestigations'].forEach((k, v) { rows.add(Padding(padding: const EdgeInsets.only(left: 8.0, bottom: 6), child: Text("- $k", style: const TextStyle(color: Colors.white70)))); });
    }
    if (rows.isEmpty) return const Text("No investigations", style: TextStyle(color: Colors.white70));
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: rows);
  }
}
