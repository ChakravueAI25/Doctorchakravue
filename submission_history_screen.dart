// Create new file: lib/submission_history_screen.dart

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:intl/intl.dart';
import 'submission_detail_screen.dart';

class SubmissionHistoryScreen extends StatefulWidget {
  final String doctorId;
  final String baseUrl;

  const SubmissionHistoryScreen({
    super.key,
    required this.doctorId,
    required this.baseUrl,
  });

  @override
  State<SubmissionHistoryScreen> createState() => _SubmissionHistoryScreenState();
}

class _SubmissionHistoryScreenState extends State<SubmissionHistoryScreen> {
  List<Map<String, dynamic>> _allSubmissions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchHistory();
  }

  Future<void> _fetchHistory() async {
    setState(() { _isLoading = true; });
    try {
      final response = await http.get(Uri.parse("${widget.baseUrl}/submissions/doctor/${widget.doctorId}/history"));
      if (response.statusCode == 200) {
        final data = json.decode(response.body) as List;
        setState(() {
          _allSubmissions = data.map((item) => item as Map<String, dynamic>).toList();
        });
      }
    } catch (e) {
      // Handle error
    } finally {
      if (mounted) setState(() { _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Full Submission History"),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _allSubmissions.isEmpty
              ? const Center(child: Text("No submission history found."))
              : RefreshIndicator(
                  onRefresh: _fetchHistory,
                  child: ListView.builder(
                    itemCount: _allSubmissions.length,
                    itemBuilder: (context, index) {
                      final submission = _allSubmissions[index];
                      final patientName = submission['patient_name'] ?? 'Unknown';
                      final timestamp = DateFormat('dd-MM-yyyy hh:mm a').format(DateTime.parse(submission['timestamp']).toLocal());
                      final bool isArchived = submission['is_archived'] ?? false;

                      return ListTile(
                        leading: Icon(
                          isArchived ? Icons.archive : Icons.inbox,
                          color: isArchived ? Colors.grey : Colors.blue,
                        ),
                        title: Text(patientName),
                        subtitle: Text("Pain: ${submission['pain_scale']}/10 on $timestamp"),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () {
                          Navigator.of(context).push(MaterialPageRoute(builder: (_) => SubmissionDetailScreen(submission: submission, baseUrl: widget.baseUrl, doctorId: widget.doctorId)));
                        },
                      );
                    },
                  ),
                ),
    );
  }
}