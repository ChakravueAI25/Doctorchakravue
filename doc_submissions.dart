import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

class SlitlampSubmission {
  final String id;
  final String docName;
  final String? fileName;
  final String? filePath;
  final String? imageData; // base64
  final Map<String, dynamic>? patientDetails;
  final DateTime? timestamp;

  SlitlampSubmission({
    required this.id,
    required this.docName,
    this.fileName,
    this.filePath,
    this.imageData,
    this.patientDetails,
    this.timestamp,
  });

  factory SlitlampSubmission.fromJson(Map<String, dynamic> j) {
    DateTime? t;
    try {
      if (j['timestamp'] != null) t = DateTime.parse(j['timestamp']).toLocal();
    } catch (_) {}
    return SlitlampSubmission(
      id: (j['_id'] ?? j['submission_id'] ?? j['id'] ?? '').toString(),
      docName: j['doc_name']?.toString() ?? j['docName']?.toString() ?? 'Document',
      fileName: j['file_name']?.toString() ?? j['fileName']?.toString(),
      filePath: j['file_path']?.toString(),
      imageData: j['image_data']?.toString(),
      patientDetails: (j['patient_details'] is Map) ? Map<String, dynamic>.from(j['patient_details']) : null,
      timestamp: t,
    );
  }
}

Future<List<SlitlampSubmission>> fetchDocSubmissions(String baseUrl, {String? doctorId}) async {
  try {
    final uri = Uri.parse(baseUrl + '/doc-submissions' + (doctorId != null ? '?doctorId=${Uri.encodeComponent(doctorId)}' : ''));
    final resp = await http.get(uri);
    if (resp.statusCode != 200) return [];
    final List<dynamic> list = json.decode(resp.body) as List<dynamic>;
    return list.map((e) => SlitlampSubmission.fromJson(e as Map<String, dynamic>)).toList();
  } catch (_) {
    return [];
  }
}

Future<bool> sendDocSubmissionMessage(String baseUrl, String submissionId, String doctorId, String message) async {
  try {
    final uri = Uri.parse(baseUrl + '/doc-submissions/' + Uri.encodeComponent(submissionId) + '/messages');
    final body = json.encode({
      'from': 'doctor',
      'doctorId': doctorId,
      'message': message,
      'timestamp': DateTime.now().toUtc().toIso8601String(),
    });
    final resp = await http.post(uri, headers: {'Content-Type': 'application/json'}, body: body);
    return resp.statusCode == 200 || resp.statusCode == 201;
  } catch (_) {
    return false;
  }
}

class DocSubmissionsScreen extends StatefulWidget {
  final String baseUrl;
  final String doctorId;
  const DocSubmissionsScreen({super.key, required this.baseUrl, required this.doctorId});

  @override
  State<DocSubmissionsScreen> createState() => _DocSubmissionsScreenState();
}

class _DocSubmissionsScreenState extends State<DocSubmissionsScreen> {
  late Future<List<SlitlampSubmission>> _future;

  @override
  void initState() {
    super.initState();
    _future = fetchDocSubmissions(widget.baseUrl, doctorId: widget.doctorId);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Slitlamp Submissions')),
      body: FutureBuilder<List<SlitlampSubmission>>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
          final list = snap.data ?? [];
          if (list.isEmpty) return const Center(child: Text('No submissions found'));
          return RefreshIndicator(
            onRefresh: () async {
              setState(() {
                _future = fetchDocSubmissions(widget.baseUrl, doctorId: widget.doctorId);
              });
            },
            child: ListView.builder(
              itemCount: list.length,
              itemBuilder: (context, i) {
                final s = list[i];
                final name = s.patientDetails?['name'] ?? 'Unknown';
                final ts = s.timestamp != null ? '${s.timestamp}' : 'Unknown';
                return Card(
                  child: ListTile(
                    title: Text(name.toString(), style: const TextStyle(color: Colors.white)),
                    subtitle: Text('${s.docName} • ${ts}', style: const TextStyle(color: Colors.white70)),
                    trailing: const Icon(Icons.chevron_right, color: Colors.white70),
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => DocSubmissionDetailScreen(baseUrl: widget.baseUrl, doctorId: widget.doctorId, submission: s))),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class DocSubmissionDetailScreen extends StatefulWidget {
  final String baseUrl;
  final String doctorId;
  final SlitlampSubmission submission;
  const DocSubmissionDetailScreen({super.key, required this.baseUrl, required this.doctorId, required this.submission});

  @override
  State<DocSubmissionDetailScreen> createState() => _DocSubmissionDetailScreenState();
}

class _DocSubmissionDetailScreenState extends State<DocSubmissionDetailScreen> {
  Uint8List? _imageBytes;
  bool _loadingImage = false;
  final TextEditingController _controller = TextEditingController();
  final List<Map<String, dynamic>> _messages = [];
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _loadImageIfNeeded();
  }

  Future<void> _loadImageIfNeeded() async {
    if (widget.submission.imageData != null && widget.submission.imageData!.isNotEmpty) {
      try {
        final cleaned = widget.submission.imageData!.replaceAll(RegExp(r"^data:image\/[^;]+;base64,"), '');
        final bytes = base64Decode(cleaned);
        setState(() => _imageBytes = bytes);
        return;
      } catch (_) {}
    }
    // try fetching file endpoint: /doc-submissions/{id}/file
    if (widget.submission.id.isNotEmpty) {
      setState(() => _loadingImage = true);
      try {
        final uri = Uri.parse(widget.baseUrl + '/doc-submissions/' + Uri.encodeComponent(widget.submission.id) + '/file');
        final resp = await http.get(uri);
        if (resp.statusCode == 200 && resp.bodyBytes.isNotEmpty) {
          setState(() => _imageBytes = resp.bodyBytes);
        }
      } catch (_) {}
      setState(() => _loadingImage = false);
    }
  }

  Future<void> _sendMessage() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    setState(() => _sending = true);
    final ok = await sendDocSubmissionMessage(widget.baseUrl, widget.submission.id, widget.doctorId, text);
    setState(() => _sending = false);
    if (ok) {
      _messages.add({'from': 'doctor', 'message': text, 'ts': DateTime.now().toIso8601String()});
      _controller.clear();
      setState(() {});
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Message sent to dashboard')));
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to send message')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final patient = widget.submission.patientDetails ?? {};
    final patientName = patient['name'] ?? 'Patient';
    return Scaffold(
      appBar: AppBar(title: Text('$patientName - ${widget.submission.docName}')),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(12),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                if (_loadingImage) const Center(child: CircularProgressIndicator()) else if (_imageBytes != null)
                  Center(child: Image.memory(_imageBytes!, fit: BoxFit.contain))
                else
                  Card(child: Padding(padding: const EdgeInsets.all(12), child: Text('No image available'))),
                const SizedBox(height: 12),
                Text('Patient: $patientName', style: const TextStyle(fontWeight: FontWeight.bold)),
                if (patient['age'] != null) Text('Age: ${patient['age']}'),
                if (patient['phone'] != null) Text('Phone: ${patient['phone']}'),
                const SizedBox(height: 12),
                const Divider(),
                const SizedBox(height: 8),
                const Text('Messages', style: TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 8),
                ..._messages.map((m) => ListTile(
                      title: Text(m['message'] ?? ''),
                      subtitle: Text(m['from'] ?? ''),
                    )),
              ]),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Row(children: [
              Expanded(child: TextField(controller: _controller, decoration: const InputDecoration(hintText: 'Message to dashboard'))),
              const SizedBox(width: 8),
              ElevatedButton(onPressed: _sending ? null : _sendMessage, child: _sending ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black)) : const Text('Send'))
            ]),
          )
        ],
      ),
    );
  }
}
