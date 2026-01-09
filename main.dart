// You can replace your entire main.dart file with this code.

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'dart:io';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:intl/intl.dart';
import 'submission_history_screen.dart';
import 'notifications_screen.dart';
import 'patient_record_screen.dart';
import 'doc_submissions.dart';
import 'adherence_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';


String appBaseUrl = "https://doctor.chakravue.co.in";

// --------------------- FCM / local notifications ---------------------
// background handler must be a top-level function
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  // You can perform background processing here if needed
  // (e.g. write to local DB)
  print('FCM bg message: ${message.messageId} ${message.data}');
}

final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

const AndroidNotificationChannel _androidChannel = AndroidNotificationChannel(
  'doctor_app_high_importance', // id
  'Doctor App Notifications', // title
  description: 'Important notifications from your doctor', // description
  importance: Importance.high,
);

Future<void> _showLocalNotification(RemoteMessage message) async {
  final n = message.notification;
  if (n == null) return;
  final title = n.title ?? message.data['title'] ?? '';
  final body = n.body ?? message.data['body'] ?? '';

  final androidDetails = AndroidNotificationDetails(
    _androidChannel.id,
    _androidChannel.name,
    channelDescription: _androidChannel.description,
    importance: Importance.high,
    priority: Priority.high,
  );
  final platform = NotificationDetails(android: androidDetails);
  await flutterLocalNotificationsPlugin.show(
    message.hashCode,
    title,
    body,
    platform,
    payload: json.encode(message.data),
  );
}

// Register token to backend for doctor
Future<void> registerFcmTokenForDoctor(String doctorId) async {
  try {
    final fm = FirebaseMessaging.instance;
    final token = await fm.getToken();
    if (token == null) return;
    final uri = Uri.parse("$appBaseUrl/doctors/$doctorId/fcm-token");
    await http.post(uri, headers: {'Content-Type': 'application/x-www-form-urlencoded'}, body: {'token': token});
    print('Registered doctor token to $uri');
  } catch (e) {
    print('Failed to register doctor token: $e');
  }
}
// --------------------------------------------------------------------

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static const Color _gold = Color(0xFFFFD700);
  static const Color _darkBg = Color(0xFF0B0F12);
  static const Color _card = Color(0xFF111418);
  static final ColorScheme _goldScheme = ColorScheme.dark(
    primary: _gold,
    onPrimary: Colors.black,
    secondary: _gold,
    background: _darkBg,
    surface: _card,
    onSurface: Colors.white70,
  );

  @override
  Widget build(BuildContext context) {
    final base = ThemeData.dark(useMaterial3: true);
    final theme = base.copyWith(
      colorScheme: _goldScheme,
      scaffoldBackgroundColor: _darkBg,
      primaryColor: _gold,
      appBarTheme: AppBarTheme(
        backgroundColor: _card,
        elevation: 2,
        iconTheme: const IconThemeData(color: Colors.white),
        titleTextStyle: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w600),
      ),
      cardTheme: CardThemeData(
        color: _card,
        elevation: 6,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: const EdgeInsets.symmetric(vertical: 6, horizontal: 8),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: _gold,
          foregroundColor: Colors.black,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: _gold,
          side: const BorderSide(color: _gold),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFF0F1416),
        labelStyle: const TextStyle(color: Colors.white70),
        hintStyle: const TextStyle(color: Colors.white38),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: Colors.transparent),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: BorderSide(color: _gold, width: 1.5),
        ),
      ),
      listTileTheme: const ListTileThemeData(
        tileColor: null,
        textColor: Colors.white70,
        iconColor: Colors.white70,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: _gold.withOpacity(0.12),
        selectedColor: _gold.withOpacity(0.22),
        labelStyle: const TextStyle(color: Colors.white),
        secondaryLabelStyle: const TextStyle(color: Colors.black),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
      textTheme: base.textTheme.apply(bodyColor: Colors.white, displayColor: Colors.white),
    );

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Doctor Dashboard',
      theme: theme,
      home: const LoginScreen(),
    );
  }
}

// --- EXISTING ---
// Login screen now makes a real API call.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController userController = TextEditingController(); // email
  final TextEditingController passController = TextEditingController();
  bool isLoading = false;
  String? error;

  @override
  void initState() {
    super.initState();
    _tryAutoLogin();
  }

  Future<void> _tryAutoLogin() async {
    final prefs = await SharedPreferences.getInstance();
    final savedId = prefs.getString('doctorId');
    final savedName = prefs.getString('doctorName');
    if (savedId != null && savedName != null && savedId.isNotEmpty) {
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => DoctorDashboard(doctorName: savedName, doctorId: savedId),
        ),
      );
    }
  }

  void _submit() async {
    setState(() {
      error = null;
      isLoading = true;
    });

    final email = userController.text.trim();
    final pass = passController.text;
    if (email.isEmpty || pass.isEmpty) {
      setState(() {
        error = "Enter email and password";
        isLoading = false;
      });
      return;
    }

    try {
      final response = await http.post(
        Uri.parse("$appBaseUrl/login/doctor"),
        headers: {"Content-Type": "application/json"},
        body: json.encode({"email": email, "password": pass}),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final String doctorName = data['name'];
        final String doctorId = data['_id'];

        // persist for auto-login
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('doctorName', doctorName);
        await prefs.setString('doctorId', doctorId);

        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => DoctorDashboard(
              doctorName: doctorName,
              doctorId: doctorId,
            ),
          ),
        );
      } else {
        final errorData = json.decode(response.body);
        setState(() {
          error = errorData['detail'] ?? 'Login failed';
        });
      }
    } catch (e) {
      setState(() {
        error = "Network error. Please try again.";
      });
    } finally {
      setState(() {
        isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 480),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    "Doctor Login",
                    style: Theme.of(context)
                        .textTheme
                        .titleLarge
                        ?.copyWith(color: Colors.white, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: userController,
                    decoration: const InputDecoration(labelText: "Email"),
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: passController,
                    decoration: const InputDecoration(labelText: "Password"),
                    obscureText: true,
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: 12),
                  if (error != null)
                    Text(error!, style: const TextStyle(color: Colors.redAccent)),
                  const SizedBox(height: 12),

                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: isLoading ? null : _submit,
                      child: isLoading
                          ? const SizedBox(
                              height: 16,
                              width: 16,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black),
                            )
                          : const Text("Login"),
                    ),
                  ),


                  if (100 == 0)
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton(
                        onPressed: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(builder: (_) => const RegisterScreen()),
                          );
                        },
                        child: const Text("Register as Doctor"),
                      ),
                    ),
                  
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}


// --- EXISTING ---
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final TextEditingController nameController = TextEditingController();
  final TextEditingController specController = TextEditingController();
  final TextEditingController emailController = TextEditingController();
  final TextEditingController phoneController = TextEditingController();
  final TextEditingController passController = TextEditingController();
  final TextEditingController confirmController = TextEditingController();

  bool isLoading = false;
  String? error;

  static const String ngrokUrl = "https://doctor.chakravue.co.in";

  Future<void> _register() async {
    setState(() {
      error = null;
    });
    final name = nameController.text.trim();
    final spec = specController.text.trim();
    final email = emailController.text.trim();
    final phone = phoneController.text.trim();
    final pass = passController.text;
    final confirm = confirmController.text;

    if (name.isEmpty || spec.isEmpty || email.isEmpty || pass.isEmpty) {
      setState(() {
        error = "Fill all required fields (name, specialization, email, password)";
      });
      return;
    }
    if (pass != confirm) {
      setState(() {
        error = "Passwords do not match";
      });
      return;
    }

    setState(() => isLoading = true);
    try {
      final uri = Uri.parse("$ngrokUrl/doctors");
      final resp = await http.post(
        uri,
        headers: {"Content-Type": "application/json"},
        body: json.encode({
          "name": name,
          "specialization": spec,
          "email": email,
          "phone": phone,
          "password": pass,
        }),
      );

      if (resp.statusCode == 200 || resp.statusCode == 201) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Registered successfully")));
        await Future.delayed(const Duration(milliseconds: 800));
        Navigator.of(context).pop(); // back to login
      } else {
        setState(() {
          error = "Registration failed (${resp.statusCode})";
        });
      }
    } catch (e) {
      setState(() {
        error = "Network error";
      });
    } finally {
      setState(() => isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Register Doctor")),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 640),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(controller: nameController, decoration: const InputDecoration(labelText: "Full name")),
                    const SizedBox(height: 8),
                    TextField(controller: specController, decoration: const InputDecoration(labelText: "Specialization")),
                    const SizedBox(height: 8),
                    TextField(controller: emailController, decoration: const InputDecoration(labelText: "Email"), keyboardType: TextInputType.emailAddress),
                    const SizedBox(height: 8),
                    TextField(controller: phoneController, decoration: const InputDecoration(labelText: "Phone"), keyboardType: TextInputType.phone),
                    const SizedBox(height: 8),
                    TextField(controller: passController, decoration: const InputDecoration(labelText: "Password"), obscureText: true),
                    const SizedBox(height: 8),
                    TextField(controller: confirmController, decoration: const InputDecoration(labelText: "Confirm password"), obscureText: true),
                    const SizedBox(height: 12),
                    if (error != null) Text(error!, style: const TextStyle(color: Colors.redAccent)),
                    const SizedBox(height: 8),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: isLoading ? null : _register,
                        child: isLoading ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black)) : const Text("Register"),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// DoctorDashboard and related screens (kept functionality, updated look)
class DoctorDashboard extends StatefulWidget {
  final String doctorName;
  final String doctorId;
  const DoctorDashboard({super.key, required this.doctorName, required this.doctorId});

  @override
  State<DoctorDashboard> createState() => _DoctorDashboardState();
}

class _DoctorDashboardState extends State<DoctorDashboard> {
  // baseUrl initialised from global appBaseUrl (set on login)
  final String baseUrl = appBaseUrl;
  final TextEditingController searchController = TextEditingController();
  final TextEditingController problemsController = TextEditingController();
  final TextEditingController medicinesController = TextEditingController();
  final TextEditingController replyController = TextEditingController();
  Map<String, dynamic>? patientData;
  bool isLoading = false;
  Uint8List? patientImage;
  final Set<String> acknowledgedPain = <String>{};
  List<Map<String, dynamic>> _submissions = [];
  bool _isSubmissionsLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchSubmissions();
  }

  Future<void> _fetchSubmissions() async {
    setState(() {
      _isSubmissionsLoading = true;
    });
    try {
      final response = await http.get(Uri.parse("$baseUrl/submissions/doctor/${widget.doctorId}"));
      if (response.statusCode == 200) {
        final data = json.decode(response.body) as List;
        setState(() {
          _submissions = data.map((item) => item as Map<String, dynamic>).toList();
        });
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Failed to load submissions")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Network error loading submissions")));
    } finally {
      if (mounted) setState(() {
        _isSubmissionsLoading = false;
      });
    }
  }

  Future<bool> _isValidImage(Uint8List bytes) async {
    try {
      await ui.instantiateImageCodec(bytes);
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<void> loadImageFromFaceId(String id) async {
    try {
      final base64Url = Uri.parse("$baseUrl/files/$id/base64");
      final base64Resp = await http.get(base64Url);
      if (base64Resp.statusCode == 200) {
        final Map<String, dynamic> j = json.decode(base64Resp.body);
        final String? b64 = j['base64'] as String?;
        if (b64 != null && b64.isNotEmpty) {
          final bytes = base64Decode(b64.replaceAll(RegExp(r'\s+'), ''));
          if (await _isValidImage(bytes)) {
            setState(() {
              patientImage = bytes;
            });
            return;
          }
        }
      }
      final gridFsUrl = Uri.parse("$baseUrl/files/$id");
      final gridResp = await http.get(gridFsUrl);
      if (gridResp.statusCode == 200 && gridResp.bodyBytes.isNotEmpty) {
        final bytes = gridResp.bodyBytes;
        if (await _isValidImage(bytes)) {
          setState(() {
            patientImage = bytes;
          });
          return;
        }
      }
      setState(() {
        patientImage = null;
      });
    } catch (_) {
      setState(() {
        patientImage = null;
      });
    }
  }

  String? _extractFaceFileId(dynamic src) {
    if (src == null) return null;
    if (src is String && src.trim().isNotEmpty) return src.trim();
    if (src is Map) {
      if (src.containsKey(r'$oid')) {
        final v = src[r'$oid'];
        if (v is String) return v;
      }
      if (src.containsKey('face_file_id') && src['face_file_id'] is String) {
        return (src['face_file_id'] as String).trim();
      }
    }
    return null;
  }

  Future<void> searchPatient() async {
    setState(() => isLoading = true);
    final query = searchController.text.trim();
    if (query.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Please enter a patient name or email")));
      setState(() => isLoading = false);
      return;
    }

    final response = await http.get(Uri.parse("$baseUrl/patients/search/?query=${Uri.encodeQueryComponent(query)}"));
    if (response.statusCode == 200) {
      final data = json.decode(response.body) as Map<String, dynamic>;
      setState(() {
        patientData = data;
        final pp = data["present_problems"];
        if (pp is List && pp.isNotEmpty) {
          final last = pp.last;
          problemsController.text = (last is Map ? (last["value"] ?? "") : "");
        } else {
          problemsController.clear();
        }
        final med = data["medicines"];
        if (med is List && med.isNotEmpty) {
          final last = med.last;
          medicinesController.text = (last is Map ? (last["value"] ?? "") : "");
        } else {
          medicinesController.clear();
        }
        patientImage = null;
      });
      final ff = _extractFaceFileId(patientData?["face_file_id"]);
      if (ff != null) {
        await loadImageFromFaceId(ff);
      }
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Patient not found")));
      setState(() {
        patientData = null;
        patientImage = null;
      });
    }
    setState(() => isLoading = false);
  }

  Future<void> saveDetails() async {
    if (patientData == null) return;
    final email = patientData!["email"] as String;
    bool okProblems = true;
    bool okMeds = true;

    final ppText = problemsController.text.trim();
    if (ppText.isNotEmpty) {
      try {
        final resp = await http.post(
          Uri.parse("$baseUrl/patients/$email/problems"),
          headers: {"Content-Type": "application/json"},
          body: json.encode({"problem": ppText}),
        );
        okProblems = resp.statusCode == 200 || resp.statusCode == 201;
      } catch (_) {
        okProblems = false;
      }
    }

    final medText = medicinesController.text.trim();
    if (medText.isNotEmpty) {
      try {
        final resp = await http.post(
          Uri.parse("$baseUrl/patients/$email/medicines"),
          headers: {"Content-Type": "application/json"},
          body: json.encode({"medicine": medText}),
        );
        okMeds = resp.statusCode == 200 || resp.statusCode == 201;
      } catch (_) {
        okMeds = false;
      }
    }

    if (!okProblems || !okMeds) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Failed to save some details")));
      return;
    }

    try {
      final resp = await http.get(Uri.parse("$baseUrl/patients/search/?query=${Uri.encodeQueryComponent(email)}"));
      if (resp.statusCode == 200) {
        final data = json.decode(resp.body) as Map<String, dynamic>;
        setState(() {
          patientData = data;
          problemsController.clear();
          medicinesController.clear();
          patientData?["present_problems"] = <dynamic>[];
          patientData?["medicines"] = <dynamic>[];
        });
        final ff = _extractFaceFileId(patientData?["face_file_id"]);
        if (ff != null) await loadImageFromFaceId(ff);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Details saved successfully")));
      }
    } catch (_) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Saved but failed to refresh patient")));
    }
  }

  Widget buildPatientDetailsTable(Map<String, dynamic>? parsed) {
    if (parsed == null || parsed.isEmpty) {
      return const Padding(padding: EdgeInsets.all(8.0), child: Text("No sight details available"));
    }
    return DataTable(
      columns: const [DataColumn(label: Text('Field')), DataColumn(label: Text('Value'))],
      rows: parsed.entries.map((e) => DataRow(cells: [DataCell(Text(e.key)), DataCell(Text(e.value.toString()))])).toList(),
    );
  }

  Widget buildTimeline({int limit = 4}) {
    if (patientData == null) return const Text("No history available");
    final List<Map<String, dynamic>> items = [];
    final historyList = patientData!["history"];
    if (historyList is List) {
      for (final hist in historyList) {
        if (hist is Map) {
          final atRaw = hist["at"]?.toString() ?? "";
          String text = "";
          if (hist.containsKey('submission_image_id')) {
            text = "${hist['problem'] ?? 'Reviewed submission.'}\nDoctor's Note: ${hist['doctor_notes'] ?? 'No notes.'}";
          } else if (hist.containsKey('procedure_type')) {
            text = "Procedure: ${hist['procedure_type']}";
            if (hist['problem'] != null) {
              text += "\nDetails: ${hist['problem']}";
            }
          } else {
            text = hist['problem'] ?? 'General record.';
          }
          items.add({"ts": atRaw, "text": text, "raw": hist});
        }
      }
    }
    items.sort((a, b) {
      final da = DateTime.tryParse(a["ts"] ?? "") ?? DateTime.fromMillisecondsSinceEpoch(0);
      final db = DateTime.tryParse(b["ts"] ?? "") ?? DateTime.fromMillisecondsSinceEpoch(0);
      return db.compareTo(da);
    });

    final recent = items.take(limit).toList();
    if (recent.isEmpty) return const Text("No history available");

    return SizedBox(
      height: 200,
      child: ListView.separated(
        itemCount: recent.length,
        separatorBuilder: (_, __) => const Divider(),
        itemBuilder: (context, i) {
          final it = recent[i];
          final tsStr = it["ts"] ?? "";
          String timeLabel = "";
          if (tsStr.isNotEmpty) {
            final dt = DateTime.tryParse(tsStr);
            if (dt != null) timeLabel = DateFormat('dd-MM-yyyy hh:mm a').format(dt.toLocal());
          }
          return ListTile(
            dense: true,
            title: Text(it["text"] ?? "", style: const TextStyle(fontSize: 13, color: Colors.white70)),
            subtitle: Text(timeLabel, style: const TextStyle(fontSize: 12, color: Colors.white38)),
            isThreeLine: (it["text"] ?? "").contains('\n'),
          );
        },
      ),
    );
  }

  Widget _buildSubmissionsList() {
    if (_isSubmissionsLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_submissions.isEmpty) {
      return const Center(child: Text("No new patient submissions."));
    }
    return RefreshIndicator(
      onRefresh: _fetchSubmissions,
      color: Theme.of(context).colorScheme.primary,
      child: ListView.builder(
        itemCount: _submissions.length,
        itemBuilder: (context, index) {
          final submission = _submissions[index];
          final patientName = submission['patient_name'] ?? 'Unknown Patient';
          String timestamp = 'Unknown';
          try {
            timestamp = DateFormat('dd-MM-yyyy hh:mm a').format(DateTime.parse(submission['timestamp']).toLocal());
          } catch (_) {}
          final bool isViewed = submission['is_viewed'] ?? false;
          return Card(
            child: ListTile(
              leading: Icon(Icons.circle, color: isViewed ? Colors.grey.shade700 : Theme.of(context).colorScheme.primary, size: 12),
              title: Text(patientName, style: const TextStyle(color: Colors.white)),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("Submitted: $timestamp", style: const TextStyle(color: Colors.white70)),
                  const SizedBox(height: 6),
                  Text(
                    // changed label "Discharge" -> "Watering"
                    "Pain: ${submission['pain_scale'] ?? 0}/10 • Swelling: ${submission['swelling'] ?? 0} • Redness: ${submission['redness'] ?? 0} • Watering: ${submission['discharge'] ?? 0}",
                    style: const TextStyle(fontSize: 13, color: Colors.white60),
                  ),
                  if ((submission['comments'] ?? "").toString().isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 6.0),
                      child: Text(
                        "Note: ${submission['comments']}",
                        style: const TextStyle(fontSize: 12, color: Colors.white70),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                ],
              ),
              isThreeLine: true,
              trailing: const Icon(Icons.chevron_right, color: Colors.white70),
                onTap: () async {
                await Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => SubmissionDetailScreen(submission: submission, baseUrl: baseUrl, doctorId: widget.doctorId),
                  ),
                );
                _fetchSubmissions();
                if (patientData != null) {
                  searchPatient();
                }
              },
            ),
          );
        },
      ),
    );
  }

  Future<void> _sendPainMessage(String email, dynamic value, String msg) async {
    if (msg.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Enter a message")));
      return;
    }
    final uri = Uri.parse("$baseUrl/patients/$email/messages");
    try {
      final resp = await http.post(
        uri,
        headers: {"Content-Type": "application/json"},
        body: json.encode({"field": "pain_scale", "value": value, "message": msg}),
      );
      if (resp.statusCode == 200 || resp.statusCode == 201) {
        setState(() => acknowledgedPain.add(email));
        replyController.clear();
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Message sent")));
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Failed to send message")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Network error")));
    }
  }

  void _showPainDialog(Map<String, dynamic> patient) {
    final email = patient["email"]?.toString() ?? "";
    final updates = patient["pain_scale"];
    final List<Map<String, String>> recent = [];
    if (updates is List) {
      for (final u in updates.reversed) {
        if (u is Map) {
          recent.add({"value": u["value"]?.toString() ?? u["v"]?.toString() ?? "", "at": u["at"]?.toString() ?? ""});
        } else {
          recent.add({"value": u?.toString() ?? "", "at": ""});
        }
        if (recent.length >= 6) break;
      }
    }

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: Theme.of(context).cardTheme.color,
        title: Text("Pain scale - ${patient["name"] ?? email}", style: const TextStyle(color: Colors.white)),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (recent.isEmpty) const Text("No pain_scale entries", style: TextStyle(color: Colors.white70)),
              if (recent.isNotEmpty)
                SizedBox(
                  height: 180,
                  child: ListView.separated(
                    itemCount: recent.length,
                    separatorBuilder: (_, __) => const Divider(color: Colors.white12),
                    itemBuilder: (context, i) {
                      final it = recent[i];
                      final ts = it["at"] ?? "";
                      String timeLabel = "";
                      if (ts.isNotEmpty) {
                        final dt = DateTime.tryParse(ts);
                        if (dt != null) timeLabel = DateFormat('dd-MM-yyyy hh:mm a').format(dt.toLocal());
                      }
                      return ListTile(
                        title: Text("Value: ${it["value"]}", style: const TextStyle(color: Colors.white)),
                        subtitle: Text(timeLabel, style: const TextStyle(color: Colors.white54)),
                        onTap: () {
                          replyController.text = "Regarding pain value ${it["value"]}: ";
                        },
                      );
                    },
                  ),
                ),
              const SizedBox(height: 8),
              TextField(
                controller: replyController,
                decoration: const InputDecoration(border: OutlineInputBorder(), hintText: "Type message to patient..."),
                maxLines: 3,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text("Cancel")),
          ElevatedButton(
            onPressed: () {
              final latestValue = recent.isNotEmpty ? recent.first["value"] : "";
              _sendPainMessage(email, latestValue, replyController.text.trim());
            },
            child: const Text("Send"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Dr. ${widget.doctorName}"),
        actions: [
          IconButton(
            tooltip: "Logout",
            onPressed: () {
              Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const LoginScreen()));
            },
            icon: const Icon(Icons.logout),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 20),
            child: ElevatedButton.icon(
              onPressed: () async {
                final response = await http.get(Uri.parse("$baseUrl/patients"));
                if (response.statusCode == 200) {
                  final data = json.decode(response.body) as List<dynamic>;
                  showDialog(
                    context: context,
                    builder: (_) => AlertDialog(
                      backgroundColor: Theme.of(context).cardTheme.color,
                      title: const Text("All Patients", style: TextStyle(color: Colors.white)),
                      content: SizedBox(
                        width: double.maxFinite,
                        child: ListView.builder(
                          itemCount: data.length,
                          itemBuilder: (context, index) {
                            final item = data[index] as Map<String, dynamic>;
                            final name = item["name"]?.toString() ?? "";
                            final email = item["email"]?.toString() ?? "";
                            final painList = item["pain_scale"];
                            final hasNotification = (painList is List && painList.isNotEmpty) && !acknowledgedPain.contains(email);
                            return ListTile(
                              title: Text(name, style: const TextStyle(color: Colors.white)),
                              subtitle: Text(email, style: const TextStyle(color: Colors.white70)),
                              onTap: () async {
                                Navigator.of(context).pop();
                                searchController.text = email.isNotEmpty ? email : name;
                                await searchPatient();
                              },
                              trailing: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  if (hasNotification)
                                    IconButton(
                                      icon: Stack(
                                        alignment: Alignment.topRight,
                                        children: const [
                                          Icon(Icons.notifications, color: Colors.red),
                                          Positioned(
                                            right: 0,
                                            child: CircleAvatar(radius: 6, backgroundColor: Colors.white),
                                          ),
                                        ],
                                      ),
                                      onPressed: () {
                                        _showPainDialog(item as Map<String, dynamic>);
                                      },
                                    ),
                                  const Icon(Icons.chevron_right, color: Colors.white70),
                                ],
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                  );
                }
              },
              icon: const Icon(Icons.people),
              label: const Text("View Patients"),
              style: ElevatedButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.primary),
            ),
          ),
          IconButton(
            tooltip: "Drug Adherence",
            icon: const Icon(Icons.medication_liquid),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => AdherenceListScreen(
                    baseUrl: baseUrl,
                    doctorId: widget.doctorId,
                  ),
                ),
              );
            },
          ),
          IconButton(
            tooltip: "Notifications",
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => NotificationsScreen(
                    baseUrl: baseUrl,
                    doctorId: widget.doctorId,
                    doctorName: widget.doctorName,
                  ),
                ),
              );
            },
            icon: const Icon(Icons.notifications),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: searchController,
                    decoration: InputDecoration(
                      hintText: "Search patient for full history...",
                      prefixIcon: const Icon(Icons.search),
                    ),
                    onSubmitted: (_) => searchPatient(),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(onPressed: searchPatient, child: const Text("Search")),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  onPressed: () {
                    Navigator.of(context).push(MaterialPageRoute(builder: (_) => DocSubmissionsScreen(baseUrl: baseUrl, doctorId: widget.doctorId)));
                  },
                  icon: const Icon(Icons.camera_alt),
                  label: const Text('Slitlamp'),
                  style: ElevatedButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.primary),
                ),
              ],
            ),
            const SizedBox(height: 10),
            const Divider(thickness: 1, color: Colors.white12),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text("Recent Submissions", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
                TextButton.icon(
                  icon: const Icon(Icons.history, color: Colors.white70),
                  label: const Text("View Full History", style: TextStyle(color: Colors.white70)),
                  style: TextButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    minimumSize: const Size(0, 36),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    visualDensity: VisualDensity.compact,
                  ),
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => SubmissionHistoryScreen(
                          doctorId: widget.doctorId,
                          baseUrl: baseUrl,
                        ),
                      ),
                    );
                  },
                )
              ],
            ),
            const SizedBox(height: 10),
            Expanded(
              child: isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : patientData != null
                      ? SingleChildScrollView(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text("Full History for ${patientData!['name']}", style: Theme.of(context).textTheme.titleLarge),
                              const SizedBox(height: 10),
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Container(
                                    width: 110,
                                    height: 110,
                                    decoration: BoxDecoration(borderRadius: BorderRadius.circular(8), color: const Color(0xFF0F1416)),
                                    child: ClipRRect(
                                      borderRadius: BorderRadius.circular(8),
                                      child: (patientImage != null
                                          ? Image.memory(patientImage!, width: 110, height: 110, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const Icon(Icons.broken_image))
                                          : const Center(child: Icon(Icons.person, size: 48, color: Colors.white38))),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: buildPatientDetailsTable(
                                      (patientData?["documents"] is List && (patientData!["documents"] as List).isNotEmpty)
                                          ? (patientData!["documents"] as List).last["parsed"] as Map<String, dynamic>?
                                          : null,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 20),
                              Text("History:", style: Theme.of(context).textTheme.titleMedium),
                              const SizedBox(height: 5),
                              buildTimeline(),
                              const SizedBox(height: 15),
                              Text("Present Problems:", style: Theme.of(context).textTheme.titleMedium),
                              const SizedBox(height: 5),
                              TextField(
                                controller: problemsController,
                                decoration: const InputDecoration(border: OutlineInputBorder(), hintText: "Enter present problems..."),
                                maxLines: 3,
                              ),
                              const SizedBox(height: 15),
                              Text("Medicines Suggested:", style: Theme.of(context).textTheme.titleMedium),
                              const SizedBox(height: 5),
                              TextField(
                                controller: medicinesController,
                                decoration: const InputDecoration(border: OutlineInputBorder(), hintText: "Enter medicines..."),
                                maxLines: 3,
                              ),
                              const SizedBox(height: 20),
                              ElevatedButton.icon(
                                onPressed: saveDetails,
                                icon: const Icon(Icons.save),
                                label: const Text("Save Details"),
                                style: ElevatedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
                              ),
                            ],
                          ),
                        )
                      : _buildSubmissionsList(),
            ),
          ],
        ),
      ),
    );
  }
}

class SubmissionDetailScreen extends StatefulWidget {
  final Map<String, dynamic> submission;
  final String baseUrl;
  final String? doctorId;

  const SubmissionDetailScreen({
    super.key,
    required this.submission,
    required this.baseUrl,
    this.doctorId,
  });

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
      // Prefer email, then id, then patient_name search
      final submission = widget.submission;
      final String? email = (submission['patient_email'] ?? submission['email'])?.toString();
      final String? pid = (submission['patient_id'] ?? submission['patient'] ?? submission['patient_oid'])?.toString();
      Map<String, dynamic>? record;

      if (email != null && email.isNotEmpty) {
        final resp = await http.get(Uri.parse("${widget.baseUrl}/patients/search/?query=${Uri.encodeQueryComponent(email)}"));
        if (resp.statusCode == 200) record = json.decode(resp.body) as Map<String, dynamic>;
      } else if (pid != null && pid.isNotEmpty) {
        final resp = await http.get(Uri.parse("${widget.baseUrl}/patients/$pid"));
        if (resp.statusCode == 200) record = json.decode(resp.body) as Map<String, dynamic>;
      } else {
        final name = submission['patient_name']?.toString() ?? '';
        if (name.isNotEmpty) {
          final resp = await http.get(Uri.parse("${widget.baseUrl}/patients/search/?query=${Uri.encodeQueryComponent(name)}"));
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

  Widget _infoCard(String title, Widget child) {
    return Card(
      color: Theme.of(context).cardTheme.color,
      margin: const EdgeInsets.symmetric(vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.w700, color: Colors.white)),
            const SizedBox(height: 8),
            child,
          ],
        ),
      ),
    );
  }

  Widget _buildComplaints(Map<String, dynamic> p) {
    final complaints = (p['presentingComplaints']?['complaints'] as List?) ?? [];
    if (complaints.isEmpty) return const Text("No presenting complaints", style: TextStyle(color: Colors.white70));
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: complaints.map((c) {
        final m = c is Map ? c : {'complaint': c.toString()};
        return Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text("- ${m['complaint'] ?? ''}${(m['duration'] != null && (m['duration'] as String).isNotEmpty) ? ' • ${m['duration']}' : ''}", style: const TextStyle(color: Colors.white70)),
        );
      }).toList(),
    );
  }

  Widget _buildMedicines(Map<String, dynamic> p) {
    final meds = (p['drugHistory']?['currentMeds'] as List?) ?? (p['medications'] as List?) ?? [];
    if (meds.isEmpty) return const Text("No current medicines", style: TextStyle(color: Colors.white70));
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: meds.map((m) {
        final mm = m is Map ? m : {'name': m.toString()};
        final name = mm['name'] ?? mm['drug'] ?? '';
        final dose = mm['dosage'] ?? '';
        final ind = mm['indication'] ?? '';
        return Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text("- $name${dose != '' ? ' • $dose' : ''}${ind != '' ? ' • $ind' : ''}", style: const TextStyle(color: Colors.white70)),
        );
      }).toList(),
    );
  }

  Widget _buildPrescription(Map<String, dynamic> p) {
    final pres = p['doctor']?['prescription'] ?? p['medications'] ?? {};
    if (pres is Map && pres.isEmpty) return const Text("No prescription recorded", style: TextStyle(color: Colors.white70));
    if (pres is List && pres.isEmpty) return const Text("No prescription recorded", style: TextStyle(color: Colors.white70));
    if (pres is Map) {
      // show simple key/value pairs
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: pres.entries.map((e) => Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text("- ${e.key}: ${e.value}", style: const TextStyle(color: Colors.white70)),
        )).toList(),
      );
    }
    if (pres is List) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: pres.map<Widget>((item) => Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text("- ${item.toString()}", style: const TextStyle(color: Colors.white70)),
        )).toList(),
      );
    }
    return Text(pres.toString(), style: const TextStyle(color: Colors.white70));
  }

  Widget _buildInvestigations(Map<String, dynamic> p) {
    final inv = p['investigations'] ?? {};
    if (inv is! Map || inv.isEmpty) return const Text("No investigations", style: TextStyle(color: Colors.white70));
    final List<Widget> rows = [];
    if ((inv['iop']?['iopReadings'] as List?)?.isNotEmpty ?? false) {
      final iops = inv['iop']['iopReadings'] as List;
      rows.add(Text("IOP readings:", style: const TextStyle(color: Colors.white70)));
      for (final r in iops) {
        rows.add(Padding(padding: const EdgeInsets.only(left: 8.0, bottom: 6), child: Text("- ${r['type'] ?? ''} ${r['time'] ?? ''} • OD:${r['od'] ?? ''} OS:${r['os'] ?? ''}", style: const TextStyle(color: Colors.white70))));
      }
    }
    if ((inv['ophthalmicInvestigations'] as Map?)?.isNotEmpty ?? false) {
      rows.add(const SizedBox(height: 6));
      rows.add(Text("Ophthalmic:", style: const TextStyle(color: Colors.white70)));
      inv['ophthalmicInvestigations'].forEach((k, v) {
        rows.add(Padding(padding: const EdgeInsets.only(left: 8.0, bottom: 6), child: Text("- $k", style: const TextStyle(color: Colors.white70))));
      });
    }
    if (rows.isEmpty) return const Text("No investigations", style: TextStyle(color: Colors.white70));
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: rows);
  }

  // helper to extract string id from various shapes (_id, { "$oid": "..." }, etc.)
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
      // warn but continue: ensure dashboard receives the note even when email is missing
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
        final payload = {
          'note': note,
          'author': 'doctor',
          'timestamp': DateTime.now().toUtc().toIso8601String(),
        };
        if (widget.doctorId != null) payload['doctorId'] = widget.doctorId!;
        await http.post(msgUri, headers: {"Content-Type": "application/json"}, body: json.encode(payload));
      }
    } catch (_) {
      // ignore failures here; patient message is primary
    }

    // attempt to archive submission (best-effort)
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
      final DateTime parsedDate = DateTime.parse(widget.submission['timestamp']).toLocal();
      formattedTimestamp = DateFormat('dd-MM-yyyy hh:mm a').format(parsedDate);
    }

    return Scaffold(
      appBar: AppBar(
        title: Text("$patientName's Submission"),
      ),
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
                      Text(
                        '$painScale / 10',
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(color: Colors.red, fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: Colors.red.withOpacity(0.08),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    children: [
                      const Icon(Icons.warning_amber_rounded, color: Colors.red),
                      const SizedBox(height: 4),
                      Text("$painScale/10", style: const TextStyle(color: Colors.red)),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 10,
              runSpacing: 8,
              children: [
                Chip(
                  avatar: const Icon(Icons.add_circle_outline, size: 18, color: Colors.white70),
                  label: Text('Swelling: $swelling', style: const TextStyle(color: Colors.white)),
                ),
                Chip(
                  avatar: const Icon(Icons.brightness_1, size: 12, color: Colors.white70),
                  label: Text('Redness: $redness', style: const TextStyle(color: Colors.white)),
                ),
                Chip(
                  avatar: const Icon(Icons.opacity, size: 18, color: Colors.white70),
                  label: Text('Watering: $discharge', style: const TextStyle(color: Colors.white)),
                ),
              ],
            ),
            const SizedBox(height: 18),
            if (comments.isNotEmpty) ...[
              const Text('Patient Comments', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
              const SizedBox(height: 8),
              Card(
                color: const Color(0xFF0F1416),
                child: Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: Text(comments, style: const TextStyle(color: Colors.white70)),
                ),
              ),
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
            // Patient summary cards (presenting complaints, medicines, prescription, investigations)
            if (_isPatientLoading)
              const Center(child: Padding(padding: EdgeInsets.symmetric(vertical: 12), child: CircularProgressIndicator()))
            else ...[
              if (_patientRecord == null)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8.0),
                  child: Text("No patient record found. Showing submission fields only.", style: Theme.of(context).textTheme.bodyMedium),
                ),
              // pass an empty map if _patientRecord is null so builders return their 'No ...' messages
              _infoCard("Presenting Complaints", _buildComplaints(_patientRecord ?? {})),
              _infoCard("Medicines / Current meds", _buildMedicines(_patientRecord ?? {})),
              _infoCard("Prescription / Doctor notes", _buildPrescription(_patientRecord ?? {})),
              _infoCard("Investigations", _buildInvestigations(_patientRecord ?? {})),
              if (_patientRecord != null)
                Align(
                  alignment: Alignment.centerRight,
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.open_in_new),
                    label: const Text("View Full Record"),
                    onPressed: () {
                      Navigator.of(context).push(MaterialPageRoute(builder: (_) => PatientRecordScreen(record: _patientRecord!)));
                    },
                  ),
                ),
            ],
            const Divider(thickness: 1, color: Colors.white12),
            const SizedBox(height: 12),
            const Text('Send Note to Patient:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
            const SizedBox(height: 8),
            TextField(
              controller: _noteController,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: "e.g., Please apply the prescribed ointment...",
                labelText: "Your Note",
              ),
              maxLines: 4,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _isSending ? null : _sendNote,
              icon: _isSending ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 3, color: Colors.black)) : const Icon(Icons.send),
              label: Text(_isSending ? "Sending..." : "Send Note & Archive"),
              style: ElevatedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
            ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }
}

class FcmHelper {
  static final instance = FcmHelper._();
  FcmHelper._();

  final FlutterLocalNotificationsPlugin _local = FlutterLocalNotificationsPlugin();

  Future<void> init({required String backendBaseUrl, required String patientId}) async {
    await Firebase.initializeApp();
    FirebaseMessaging.onBackgroundMessage((message) async { await Firebase.initializeApp(); });
    const AndroidInitializationSettings androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    await _local.initialize(const InitializationSettings(android: androidInit));
    FirebaseMessaging.onMessage.listen((msg) { _show(msg); });
    // register token
    final token = await FirebaseMessaging.instance.getToken();
    if (token != null) {
      await registerTokenToServer(patientId, token, backendBaseUrl);
    }
  }

  Future<void> registerTokenToServer(String patientId, String token, String backend) async {
    try {
      final uri = Uri.parse('$backend/patients/$patientId/fcm-token');
      await http.post(uri, headers: {'Content-Type': 'application/x-www-form-urlencoded'}, body: {'token': token});
    } catch (_) {}
  }

  Future<void> _show(RemoteMessage msg) async {
    final n = msg.notification;
    if (n == null) return;
    const AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
      'high', 'High importance', channelDescription: 'desc', importance: Importance.high, priority: Priority.high);
    await _local.show(msg.hashCode, n.title, n.body, NotificationDetails(android: androidDetails), payload: json.encode(msg.data));
  }

  Future<String?> getToken() => FirebaseMessaging.instance.getToken();
}