import 'package:flutter/material.dart';

class PatientRecordScreen extends StatelessWidget {
  final Map<String, dynamic> record;
  const PatientRecordScreen({super.key, required this.record});

  Widget _noData() => const Text("No data available", style: TextStyle(color: Colors.white70));

  Widget _buildValue(dynamic v) {
    if (v == null) return _noData();
    if (v is Map && v.isEmpty) return _noData();
    if (v is List && v.isEmpty) return _noData();
    if (v is Map) return _buildMap(v);
    if (v is List) return _buildList(v);
    return Text(v.toString(), style: const TextStyle(color: Colors.white70));
  }

  Widget _buildList(List list) {
    if (list.isEmpty) return _noData();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: list.asMap().entries.map((entry) {
        final idx = entry.key;
        final e = entry.value;
        if (e is Map) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text("Item ${idx + 1}:", style: const TextStyle(fontWeight: FontWeight.w700, color: Colors.white)),
                const SizedBox(height: 6),
                _buildMap(e),
              ],
            ),
          );
        } else {
          return Padding(
            padding: const EdgeInsets.only(bottom: 6.0),
            child: Text("• ${e.toString()}", style: const TextStyle(color: Colors.white70)),
          );
        }
      }).toList(),
    );
  }

  Widget _buildMap(Map m) {
    if (m.isEmpty) return _noData();
    final entries = m.entries.toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: entries.map((e) {
        final key = e.key.toString();
        final val = e.value;
        return Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 140,
                child: Text(
                  "$key:",
                  style: const TextStyle(fontWeight: FontWeight.w700, color: Colors.white),
                ),
              ),
              Expanded(child: _buildValue(val)),
            ],
          ),
        );
      }).toList(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Patient Full Record"),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(12),
        child: Card(
          color: Theme.of(context).cardTheme.color,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: record.isEmpty ? _noData() : _buildMap(record),
          ),
        ),
      ),
    );
  }
}