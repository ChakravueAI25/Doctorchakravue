import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:intl/intl.dart';

class AdherenceListScreen extends StatefulWidget {
  final String baseUrl;
  final String doctorId;

  const AdherenceListScreen({
    super.key,
    required this.baseUrl,
    required this.doctorId,
  });

  @override
  State<AdherenceListScreen> createState() => _AdherenceListScreenState();
}

class _AdherenceListScreenState extends State<AdherenceListScreen> {
  List<dynamic> _patients = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchAdherenceData();
  }

  Future<void> _fetchAdherenceData() async {
    try {
      final uri = Uri.parse("${widget.baseUrl}/doctors/${widget.doctorId}/adherence-list");
      final response = await http.get(uri);

      if (response.statusCode == 200) {
        if (mounted) {
          setState(() {
            _patients = json.decode(response.body);
            _isLoading = false;
          });
        }
      } else {
        throw "Server error ${response.statusCode}";
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Error loading data: $e")),
        );
      }
    }
  }

  String _formatLastActive(String? iso) {
    if (iso == null) return "Unknown";
    try {
      final dt = DateTime.parse(iso);
      // Convert UTC to IST (UTC+5:30)
      final ist = dt.add(const Duration(hours: 5, minutes: 30));
      // Returns "Mon, 13:30" (24-hour format, IST)
      return DateFormat('E, HH:mm').format(ist);
    } catch (_) {
      return iso;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Drug Adherence")),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _patients.isEmpty
              ? const Center(child: Text("No medication history found."))
              : ListView.builder(
                  itemCount: _patients.length,
                  itemBuilder: (context, index) {
                    final patient = _patients[index];
                    final name = patient['patient_name'] ?? "Unknown";
                    final lastTaken = _formatLastActive(patient['last_medication_at']);
                    final history = (patient['medication_history'] as List?) ?? [];
                    final count = history.length;

                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Theme.of(context).colorScheme.primary.withOpacity(0.2),
                          child: Text(
                            name.isNotEmpty ? name[0].toUpperCase() : "?",
                            style: TextStyle(color: Theme.of(context).colorScheme.primary, fontWeight: FontWeight.bold),
                          ),
                        ),
                        title: Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                        subtitle: Text("Last active: $lastTaken • $count total entries", style: const TextStyle(color: Colors.white70)),
                        trailing: const Icon(Icons.chevron_right, color: Colors.white70),
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => PatientAdherenceDetailScreen(patient: patient),
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

// ---------------------------------------------------------------------------
// NEW: Detail Screen with Daily Accordion & Progress Bar
// ---------------------------------------------------------------------------

class PatientAdherenceDetailScreen extends StatefulWidget {
  final Map<String, dynamic> patient;

  const PatientAdherenceDetailScreen({super.key, required this.patient});

  @override
  State<PatientAdherenceDetailScreen> createState() => _PatientAdherenceDetailScreenState();
}

class _PatientAdherenceDetailScreenState extends State<PatientAdherenceDetailScreen> {
  // We store the history grouped by day: {"2025-12-29": [Entry1, Entry2], ...}
  late Map<String, List<dynamic>> _groupedHistory;
  late List<String> _sortedDates;

  @override
  void initState() {
    super.initState();
    _processHistory();
  }

  void _processHistory() {
    final rawHistory = (widget.patient['medication_history'] as List?) ?? [];
    final Map<String, List<dynamic>> groups = {};

    for (var entry in rawHistory) {
      final iso = entry['created_at'] as String?;
      if (iso == null) continue;
      
      try {
        final dt = DateTime.parse(iso).toLocal();
        // Group Key: "yyyy-MM-dd" (e.g., 2025-12-29)
        final dateKey = DateFormat('yyyy-MM-dd').format(dt);
        
        if (!groups.containsKey(dateKey)) {
          groups[dateKey] = [];
        }
        groups[dateKey]!.add(entry);
      } catch (_) {
        // ignore invalid dates
      }
    }

    // Sort the list within each day by time (descending)
    groups.forEach((key, list) {
      list.sort((a, b) => (b['created_at'] ?? "").compareTo(a['created_at'] ?? ""));
    });

    _groupedHistory = groups;
    // Sort keys (dates) descending: Today first
    _sortedDates = groups.keys.toList()..sort((a, b) => b.compareTo(a));
  }

  String _getPrettyDate(String dateKey) {
    try {
      final dt = DateTime.parse(dateKey);
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final check = DateTime(dt.year, dt.month, dt.day);

      if (check == today) return "Today";
      if (check == today.subtract(const Duration(days: 1))) return "Yesterday";
      
      // Return "Mon, Dec 29"
      return DateFormat('EEE, MMM d').format(dt);
    } catch (_) {
      return dateKey;
    }
  }

  String _formatTime(String? iso) {
    if (iso == null) return "";
    try {
      final dt = DateTime.parse(iso);
      // Convert UTC to IST (UTC+5:30)
      final ist = dt.add(const Duration(hours: 5, minutes: 30));
      return DateFormat('HH:mm').format(ist);
    } catch (_) {
      return "";
    }
  }

  @override
  Widget build(BuildContext context) {
    final name = widget.patient['patient_name'] ?? "Patient";

    return Scaffold(
      appBar: AppBar(title: Text("$name's Progress")),
      body: _sortedDates.isEmpty
          ? const Center(child: Text("No history available"))
          : ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: _sortedDates.length,
              itemBuilder: (context, index) {
                final dateKey = _sortedDates[index];
                final entries = _groupedHistory[dateKey]!;
                
                // Calculate Daily Stats
                final total = entries.length;
                final takenCount = entries.where((e) => e['taken'].toString() == '1' || e['taken'] == true).length;
                final progress = total == 0 ? 0.0 : (takenCount / total);
                final isPerfect = progress == 1.0;

                // Color logic
                final color = isPerfect ? Colors.green : (progress > 0.5 ? Colors.orange : Colors.red);

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  child: Theme(
                    // Remove divider lines from ExpansionTile
                    data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
                    child: ExpansionTile(
                      initiallyExpanded: index == 0, // Open "Today" by default
                      tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      title: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                _getPrettyDate(dateKey),
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
                              ),
                              Text(
                                "$takenCount / $total Taken",
                                style: TextStyle(
                                  color: color, 
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          // Progress Bar
                          ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: LinearProgressIndicator(
                              value: progress,
                              backgroundColor: Colors.grey.shade800,
                              valueColor: AlwaysStoppedAnimation(color),
                              minHeight: 6,
                            ),
                          ),
                        ],
                      ),
                      children: entries.map((entry) {
                        final medName = entry['medicine'] ?? "Unknown";
                        final isTaken = (entry['taken'].toString() == '1' || entry['taken'] == true);
                        final timeStr = _formatTime(entry['created_at']);

                        return Container(
                          decoration: BoxDecoration(
                            color: Colors.black12,
                            border: Border(top: BorderSide(color: Colors.white.withOpacity(0.05))),
                          ),
                          child: ListTile(
                            dense: true,
                            leading: Icon(
                              isTaken ? Icons.check_circle : Icons.cancel,
                              color: isTaken ? Colors.green : Colors.red,
                            ),
                            title: Text(medName, style: const TextStyle(color: Colors.white70)),
                            trailing: Text(timeStr, style: const TextStyle(color: Colors.white38, fontSize: 12)),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
