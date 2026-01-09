import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';

class NotificationsScreen extends StatefulWidget {
  final String baseUrl;
  final String doctorId;
  final String doctorName;

  const NotificationsScreen({
    super.key,
    required this.baseUrl,
    required this.doctorId,
    required this.doctorName,
  });

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final TextEditingController _titleCtrl = TextEditingController();
  final TextEditingController _msgCtrl = TextEditingController();
  bool _sendToAll = true;
  List<Map<String, dynamic>> _patients = [];
  final Set<String> _selectedEmails = {};
  final List<XFile> _images = [];
  bool _loadingPatients = false;
  bool _sending = false;

  final ImagePicker _picker = ImagePicker();

  Future<void> _loadPatients() async {
    setState(() => _loadingPatients = true);
    try {
      final resp = await http.get(Uri.parse("${widget.baseUrl}/patients"));
      if (resp.statusCode == 200) {
        final List<dynamic> list = json.decode(resp.body) as List<dynamic>;
        setState(() {
          _patients = list.map((e) => (e as Map<String, dynamic>)).toList();
        });
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text("Failed to load patients: ${resp.statusCode}")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Network error loading patients")));
    } finally {
      setState(() => _loadingPatients = false);
    }
  }

  Future<void> _pickImage() async {
    try {
      final XFile? file = await _picker.pickImage(source: ImageSource.gallery, imageQuality: 80);
      if (file != null) {
        setState(() => _images.add(file));
      }
    } catch (e) {
      // ignore
    }
  }

  Future<void> _sendNotification() async {
    final msg = _msgCtrl.text.trim();
    if (msg.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Enter a message")));
      return;
    }

    setState(() => _sending = true);
    try {
      final uri = Uri.parse("${widget.baseUrl}/notifications");
      final request = http.MultipartRequest('POST', uri);
      request.fields['doctor_id'] = widget.doctorId;
      request.fields['doctor_name'] = widget.doctorName;
      request.fields['title'] = _titleCtrl.text.trim();
      request.fields['message'] = msg;

      final recipients = _sendToAll
          ? {"all": true}
          : {"all": false, "emails": _selectedEmails.toList()};
      request.fields['recipients'] = json.encode(recipients);

      for (int i = 0; i < _images.length; i++) {
        final XFile xf = _images[i];
        final file = File(xf.path);
        final filename = xf.name;
        // backend expects multipart files under the "files" field
        request.files.add(await http.MultipartFile.fromPath('files', file.path, filename: filename));
      }

      final streamed = await request.send();
      final respStr = await streamed.stream.bytesToString();
      if (streamed.statusCode == 200 || streamed.statusCode == 201) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Notification created and queued")));
        Navigator.of(context).pop();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Failed: ${streamed.statusCode} $respStr")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Network error: $e")));
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Widget _buildPatientsSelector() {
    if (_loadingPatients) return const Center(child: CircularProgressIndicator());
    if (_patients.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ElevatedButton(
            onPressed: _loadPatients,
            child: const Text("Load patients"),
          ),
          const SizedBox(height: 8),
          const Text("No patients loaded"),
        ],
      );
    }

    return SizedBox(
      height: 260,
      child: ListView.builder(
        itemCount: _patients.length,
        itemBuilder: (context, i) {
          final p = _patients[i];
          final email = (p['email'] ?? '').toString();
          final name = (p['name'] ?? email);
          final checked = _selectedEmails.contains(email);
          return CheckboxListTile(
            value: checked,
            onChanged: (v) {
              setState(() {
                if (v == true) {
                  _selectedEmails.add(email);
                } else {
                  _selectedEmails.remove(email);
                }
              });
            },
            title: Text(name),
            subtitle: Text(email),
          );
        },
      ),
    );
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _msgCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Compose Notification"),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(controller: _titleCtrl, decoration: const InputDecoration(labelText: "Title (optional)")),
            const SizedBox(height: 12),
            TextField(
              controller: _msgCtrl,
              decoration: const InputDecoration(labelText: "Message"),
              maxLines: 6,
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Text("Recipients:", style: TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(width: 12),
                Expanded(
                  child: Row(
                    children: [
                      Radio<bool>(value: true, groupValue: _sendToAll, onChanged: (v) => setState(() => _sendToAll = true)),
                      const Text("All"),
                      const SizedBox(width: 12),
                      Radio<bool>(value: false, groupValue: _sendToAll, onChanged: (v) => setState(() => _sendToAll = false)),
                      const Text("Select"),
                    ],
                  ),
                ),
              ],
            ),
            if (!_sendToAll) ...[
              _buildPatientsSelector(),
              const SizedBox(height: 8),
              Text("Selected: ${_selectedEmails.length}", style: const TextStyle(color: Colors.white70)),
            ],
            const SizedBox(height: 12),
            const Text("Images (optional):", style: TextStyle(fontWeight: FontWeight.w600)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                ElevatedButton.icon(
                  onPressed: _pickImage,
                  icon: const Icon(Icons.photo_library),
                  label: const Text("Add image"),
                ),
                if (_images.isNotEmpty)
                  ..._images.map((x) {
                    return Stack(
                      alignment: Alignment.topRight,
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(top: 8.0),
                          child: Image.file(File(x.path), width: 80, height: 80, fit: BoxFit.cover),
                        ),
                        Positioned(
                          right: 0,
                          child: GestureDetector(
                            onTap: () {
                              setState(() {
                                _images.remove(x);
                              });
                            },
                            child: const CircleAvatar(radius: 12, child: Icon(Icons.close, size: 14)),
                          ),
                        ),
                      ],
                    );
                  }).toList(),
              ],
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _sending ? null : _sendNotification,
                icon: _sending ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.send),
                label: Text(_sending ? "Sending..." : "Send Notification"),
              ),
            ),
          ],
        ),
      ),
    );
  }
}