import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../utils/api_config.dart';

class EmailScreen extends StatefulWidget {
  const EmailScreen({Key? key}) : super(key: key);

  @override
  State<EmailScreen> createState() => _EmailScreenState();
}

class _EmailScreenState extends State<EmailScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _appPasswordController = TextEditingController();
  
  bool _isFetching = false;
  bool _isScanning = false;
  List<dynamic> _emails = [];
  String? _errorMessage;

  Future<void> _fetchEmails() async {
    final email = _emailController.text.trim();
    final appPassword = _appPasswordController.text.trim();

    if (email.isEmpty || appPassword.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter both email and app password.')),
      );
      return;
    }

    setState(() {
      _isFetching = true;
      _errorMessage = null;
      _emails = [];
    });

    try {
      final baseUrl = await ApiConfig.getBaseUrl();
      final response = await http.post(
        Uri.parse('$baseUrl/api/gmail/fetch'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'app_password': appPassword,
        }),
      ).timeout(const Duration(seconds: 60));
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['error'] != null) {
          setState(() {
            _errorMessage = data['error'];
          });
        } else {
          setState(() {
            _emails = data['emails'] ?? [];
            if (_emails.isEmpty) {
              _errorMessage = 'No emails found in your inbox.';
            }
          });
        }
      } else {
        String serverMsg = 'Server error (${response.statusCode})';
        try {
          final data = jsonDecode(response.body);
          if (data['error'] != null) serverMsg = data['error'];
        } catch (_) {}
        setState(() {
          _errorMessage = serverMsg;
        });
      }
    } on http.ClientException catch (e) {
      setState(() {
        _errorMessage = 'Connection failed. Backend may be starting up — please try again in 30 seconds.\n(${e.message})';
      });
    } on FormatException {
      setState(() {
        _errorMessage = 'Received an invalid response from the server.';
      });
    } catch (e) {
      final msg = e.toString();
      if (msg.contains('TimeoutException')) {
        setState(() {
          _errorMessage = 'Request timed out. The server may be waking up (free tier). Please try again.';
        });
      } else if (msg.contains('SocketException') || msg.contains('Connection refused')) {
        setState(() {
          _errorMessage = 'Cannot reach the server. Check your internet connection or server URL in settings.';
        });
      } else {
        setState(() {
          _errorMessage = 'Failed to fetch emails. $msg';
        });
      }
    } finally {
      setState(() {
        _isFetching = false;
      });
    }
  }

  Future<void> _scanEmail(int index, String subject, String body) async {
    setState(() {
      _isScanning = true;
    });

    try {
      final baseUrl = await ApiConfig.getBaseUrl();
      final fullText = 'Subject: $subject\n\n$body';
      
      final response = await http.post(
        Uri.parse('$baseUrl/api/scan'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'text': fullText}),
      );
      
      if (response.statusCode == 200) {
        final result = jsonDecode(response.body);
        setState(() {
          _emails[index]['result'] = result;
          _emails[index]['status'] = 'SCANNED';
        });
      } else {
        throw Exception('Scan API failed');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to scan email: $e')),
        );
      }
    } finally {
      setState(() {
        _isScanning = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Image.asset(
              'assets/logo.png',
              height: 28,
              fit: BoxFit.contain,
            ),
            const SizedBox(width: 10),
            const Text('Email Shield', style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Connect your Gmail to auto-scan recent emails for phishing and scams.",
              style: TextStyle(color: Colors.grey, fontSize: 16),
            ),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.white10),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TextField(
                    controller: _emailController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: "Gmail Address",
                      labelStyle: TextStyle(color: Colors.grey),
                      prefixIcon: Icon(LucideIcons.mail, color: Colors.grey),
                      enabledBorder: UnderlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _appPasswordController,
                    obscureText: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: "Google App Password",
                      labelStyle: TextStyle(color: Colors.grey),
                      prefixIcon: Icon(LucideIcons.lock, color: Colors.grey),
                      enabledBorder: UnderlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton.icon(
                    onPressed: _isFetching ? null : _fetchEmails,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF10B981),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    icon: _isFetching 
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                        : const Icon(LucideIcons.refreshCw, color: Colors.white, size: 20),
                    label: const Text("Fetch Recent Emails", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            
            if (_errorMessage != null)
              Container(
                padding: const EdgeInsets.all(12),
                margin: const EdgeInsets.only(bottom: 24),
                decoration: BoxDecoration(
                  color: Colors.red.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.red.withValues(alpha: 0.5)),
                ),
                child: Text(_errorMessage!, style: const TextStyle(color: Colors.red)),
              ),
              
            if (_emails.isNotEmpty) ...[
              const Text(
                "RECENT INBOX",
                style: TextStyle(color: Colors.grey, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2),
              ),
              const SizedBox(height: 12),
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _emails.length,
                itemBuilder: (context, index) {
                  final email = _emails[index];
                  final hasResult = email['result'] != null;
                  final risk = hasResult ? email['result']['risk'] : null;
                  
                  Color statusColor = Colors.grey;
                  IconData statusIcon = LucideIcons.helpCircle;
                  
                  if (hasResult) {
                    if (risk == 'SAFE') {
                      statusColor = const Color(0xFF10B981);
                      statusIcon = LucideIcons.checkCircle;
                    } else if (risk == 'SCAM') {
                      statusColor = Colors.red;
                      statusIcon = LucideIcons.shieldAlert;
                    } else {
                      statusColor = Colors.orange;
                      statusIcon = LucideIcons.alertTriangle;
                    }
                  }

                  return Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: hasResult ? statusColor.withValues(alpha: 0.5) : Colors.white10,
                        width: hasResult ? 2 : 1,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                email['from'] ?? 'Unknown Sender',
                                style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 14),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (hasResult)
                              Row(
                                children: [
                                  Icon(statusIcon, color: statusColor, size: 16),
                                  const SizedBox(width: 4),
                                  Text(
                                    risk ?? 'UNKNOWN',
                                    style: TextStyle(color: statusColor, fontWeight: FontWeight.bold, fontSize: 12),
                                  ),
                                  if (email['result']?['confidence'] != null)
                                    Padding(
                                      padding: const EdgeInsets.only(left: 4.0),
                                      child: Text(
                                        '(${(email['result']['confidence'] * 100).round()}%)',
                                        style: TextStyle(color: statusColor.withOpacity(0.8), fontSize: 11),
                                      ),
                                    ),
                                ],
                              )
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          email['subject'] ?? 'No Subject',
                          style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          email['body'] ?? 'No preview available.',
                          style: const TextStyle(color: Colors.grey, fontSize: 12),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 12),
                        if (!hasResult)
                          Align(
                            alignment: Alignment.centerRight,
                            child: OutlinedButton.icon(
                              onPressed: _isScanning ? null : () => _scanEmail(index, email['subject'] ?? '', email['body'] ?? ''),
                              icon: const Icon(LucideIcons.scan, size: 16, color: Colors.white),
                              label: const Text("Scan for Threats", style: TextStyle(color: Colors.white)),
                              style: OutlinedButton.styleFrom(
                                side: const BorderSide(color: Colors.white24),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                              ),
                            ),
                          )
                        else ...[
                           const Divider(color: Colors.white10, height: 24),
                           Text(
                             "REASON: ${email['result']['reason'] ?? 'No reason provided'}",
                             style: TextStyle(color: statusColor, fontSize: 12),
                           ),
                        ]
                      ],
                    ),
                  );
                },
              ),
            ],
          ],
        ),
      ),
    );
  }
}
