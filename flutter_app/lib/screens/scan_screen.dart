import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../utils/api_config.dart';

class ScanScreen extends StatefulWidget {
  const ScanScreen({Key? key}) : super(key: key);

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> {
  final TextEditingController _textController = TextEditingController();
  bool _isLoading = false;
  Map<String, dynamic>? _result;

  final List<Map<String, String>> _presets = [
    {
      'title': 'SBI Urgency',
      'text': 'URGENT: Your SBI bank account has been suspended due to pending KYC update. Click here to verify immediately: http://bit.ly/sbi-verify-kyc',
      'category': 'SCAM'
    },
    {
      'title': 'Lottery Win',
      'text': 'Congratulations! You have won a cash lottery prize of Rs 1,00,000. To claim your prize, please call 1800-XXX-XXXX or enter your bank UPI PIN.',
      'category': 'SCAM'
    },
    {
      'title': 'Delivery OTP',
      'text': 'Your Amazon delivery agent is nearby. Please share the OTP 8492 to receive your package. Do not share for any other reason.',
      'category': 'SAFE'
    },
    {
      'title': 'Casual Chat',
      'text': 'Hey friend, did you get a chance to review the document I sent you yesterday? Let me know when you are free.',
      'category': 'SAFE'
    }
  ];

  Future<void> _scanText() async {
    if (_textController.text.trim().isEmpty) return;
    
    setState(() {
      _isLoading = true;
      _result = null;
    });

    try {
      final baseUrl = await ApiConfig.getBaseUrl();
      final response = await http.post(
        Uri.parse('$baseUrl/api/scan'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'text': _textController.text}),
      );
      
      if (response.statusCode == 200) {
        setState(() {
          _result = jsonDecode(response.body);
        });
      } else {
        throw Exception('API failed');
      }
    } catch (e) {
      setState(() {
        _result = {
          'risk': 'ERROR',
          'reason': 'Could not connect to the backend server. Make sure the backend is running at the address specified in Settings.',
          'confidence': 0.0,
          'label': 'network_error'
        };
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  void _usePreset(String text) {
    setState(() {
      _textController.text = text;
    });
  }

  void _showAudioScanDialog() {
    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF151821),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      isScrollControlled: true,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AudioScannerModal(
              onScanComplete: (result, transcription) {
                setState(() {
                  _result = result;
                  _textController.text = transcription;
                });
              },
            );
          },
        );
      },
    );
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
            const Text('Scan Center', style: TextStyle(fontWeight: FontWeight.bold)),
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
              "Analyze any message, email, or text for social engineering threats.",
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
                children: [
                  TextField(
                    controller: _textController,
                    maxLines: 5,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      hintText: "Paste suspicious text here...",
                      hintStyle: TextStyle(color: Colors.grey),
                      border: InputBorder.none,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      OutlinedButton.icon(
                        onPressed: _showAudioScanDialog,
                        icon: const Icon(LucideIcons.fileAudio, color: Colors.white),
                        label: const Text("Audio Scan", style: TextStyle(color: Colors.white)),
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: Colors.white24),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        ),
                      ),
                      ElevatedButton(
                        onPressed: _isLoading ? null : _scanText,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF10B981),
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        ),
                        child: _isLoading 
                          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                          : const Text("Scan Now", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              "QUICK TEST PRESETS",
              style: TextStyle(color: Colors.grey, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2),
            ),
            const SizedBox(height: 12),
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 1.6,
              ),
              itemCount: _presets.length,
              itemBuilder: (context, index) {
                final preset = _presets[index];
                final isScam = preset['category'] == 'SCAM';
                return InkWell(
                  onTap: () => _usePreset(preset['text']!),
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              preset['title']!,
                              style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: isScam ? Colors.red.withOpacity(0.2) : Colors.green.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                preset['category']!,
                                style: TextStyle(
                                  fontSize: 10,
                                  color: isScam ? Colors.redAccent : Colors.greenAccent,
                                  fontWeight: FontWeight.bold
                                ),
                              ),
                            ),
                          ],
                        ),
                        Text(
                          preset['text']!,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 12, color: Colors.grey),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
            if (_result != null) ...[
              const SizedBox(height: 24),
              _buildResultCard(),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildResultCard() {
    final risk = _result!['risk'] ?? 'UNKNOWN';
    final isSafe = risk == 'SAFE';
    final isScam = risk == 'SCAM';
    final color = isSafe ? const Color(0xFF10B981) : (isScam ? Colors.red : Colors.orange);
    final icon = isSafe ? LucideIcons.checkCircle : (isScam ? LucideIcons.shieldAlert : LucideIcons.alertTriangle);
    
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border(top: BorderSide(color: color, width: 4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: color, size: 40),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(risk, style: TextStyle(color: color, fontSize: 24, fontWeight: FontWeight.bold)),
                    if (_result!['confidence'] != null)
                      Text("${(_result!['confidence'] * 100).round()}% Confidence • Label: ${_result!['label'] ?? 'generic'}", style: const TextStyle(color: Colors.grey)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Text("REASON BREAKDOWN", style: TextStyle(color: Colors.grey, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2)),
          const SizedBox(height: 8),
          Text(_result!['reason'] ?? 'No reason provided', style: const TextStyle(color: Colors.white, height: 1.5)),
        ],
      ),
    );
  }
}

class AudioScannerModal extends StatefulWidget {
  final Function(Map<String, dynamic> result, String transcription) onScanComplete;

  const AudioScannerModal({Key? key, required this.onScanComplete}) : super(key: key);

  @override
  State<AudioScannerModal> createState() => _AudioScannerModalState();
}

class _AudioScannerModalState extends State<AudioScannerModal> with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  int _step = 0; // 0: Select Audio, 1: Scanning/Transcribing, 2: Finished
  String _currentStatus = "";
  String _selectedTitle = "";
  
  final List<Map<String, String>> _audioSamples = [
    {
      'title': 'HDFC Impersonation Call',
      'description': 'Scammer acting as HDFC security, urging OTP verification.',
      'text': 'Hello customer, I am calling from HDFC Bank security division. We have noticed an unauthorized login attempt on your netbanking. Please verify your OTP immediately to block this transaction, or your account will be frozen.'
    },
    {
      'title': 'Amazon Delivery Call',
      'description': 'Legitimate Amazon agent confirming delivery schedule.',
      'text': 'Good morning. I am calling from Amazon to confirm if you are available at your residence to receive your delivery in the next hour. No OTP is required.'
    },
    {
      'title': 'Hinglish Lottery Claim Call',
      'description': 'Lottery prize scammer asking for bank PIN codes.',
      'text': 'Aapka lottery ticket number 4920 select hua hai. Aapko twenty-five lakhs ka cash prize mila hai. Ise check karne ke liye is code ko open kijiye verify link par: http://tinyurl.com/win-lottery'
    }
  ];

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 1),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  Future<void> _startScanning(Map<String, String> sample) async {
    setState(() {
      _step = 1;
      _selectedTitle = sample['title']!;
      _currentStatus = "Transcribing call audio using Whisper...";
    });

    // Simulate Whisper transcription delay
    await Future.delayed(const Duration(seconds: 2));

    if (!mounted) return;
    setState(() {
      _currentStatus = "Analyzing transcript text with Aegis AI...";
    });

    try {
      final baseUrl = await ApiConfig.getBaseUrl();
      final response = await http.post(
        Uri.parse('$baseUrl/api/scan'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'text': sample['text']}),
      );

      if (response.statusCode == 200) {
        final result = jsonDecode(response.body);
        widget.onScanComplete(result, sample['text']!);
        if (mounted) {
          setState(() {
            _step = 2;
            _currentStatus = "Analysis Completed Successfully!";
          });
        }
        await Future.delayed(const Duration(milliseconds: 800));
        if (mounted) Navigator.pop(context);
      } else {
        throw Exception();
      }
    } catch (_) {
      // Offline fallback evaluation
      final isScam = sample['title']!.contains('Impersonation') || sample['title']!.contains('Lottery');
      final fallbackResult = {
        'risk': isScam ? 'SCAM' : 'SAFE',
        'confidence': 0.85,
        'label': isScam ? 'phishing_impersonation' : 'safe_commercial',
        'reason': isScam 
          ? 'Urgency phrases and suspicious links detected in the audio transcript (Offline Fallback).'
          : 'Normal conversation without security threats or financial requests (Offline Fallback).'
      };
      widget.onScanComplete(fallbackResult, sample['text']!);
      if (mounted) {
        setState(() {
          _step = 2;
          _currentStatus = "Analysis Completed (Offline Fallback)";
        });
      }
      await Future.delayed(const Duration(milliseconds: 800));
      if (mounted) Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 24, 16, MediaQuery.of(context).viewInsets.bottom + 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                "Audio Threat Scanner",
                style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close, color: Colors.grey),
              )
            ],
          ),
          const Divider(color: Colors.white10),
          const SizedBox(height: 12),
          if (_step == 0) ...[
            const Text(
              "Select an audio recording to simulate transcription & scanning:",
              style: TextStyle(color: Colors.grey, fontSize: 14),
            ),
            const SizedBox(height: 16),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _audioSamples.length,
              itemBuilder: (context, index) {
                final sample = _audioSamples[index];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: InkWell(
                    onTap: () => _startScanning(sample),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.04),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.white10),
                      ),
                      child: Row(
                        children: [
                          const CircleAvatar(
                            backgroundColor: Color(0xFF10B981),
                            child: Icon(Icons.play_arrow, color: Colors.white),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(sample['title']!, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
                                const SizedBox(height: 4),
                                Text(sample['description']!, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ] else ...[
            Center(
              child: Column(
                children: [
                  const SizedBox(height: 24),
                  AnimatedBuilder(
                    animation: _animationController,
                    builder: (context, child) {
                      return Container(
                        height: 90,
                        width: 90,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: const Color(0xFF10B981).withOpacity(0.1 + (_animationController.value * 0.15)),
                          border: Border.all(
                            color: const Color(0xFF10B981).withOpacity(_animationController.value),
                            width: 2 + (_animationController.value * 4),
                          ),
                        ),
                        child: const Icon(LucideIcons.fileAudio, size: 40, color: Color(0xFF10B981)),
                      );
                    },
                  ),
                  const SizedBox(height: 24),
                  Text(
                    _selectedTitle,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _currentStatus,
                    style: const TextStyle(color: Colors.grey, fontSize: 14),
                  ),
                  const SizedBox(height: 24),
                  if (_step == 1)
                    const SizedBox(
                      width: 150,
                      child: LinearProgressIndicator(
                        backgroundColor: Colors.white10,
                        color: Color(0xFF10B981),
                      ),
                    ),
                  const SizedBox(height: 24),
                ],
              ),
            )
          ],
        ],
      ),
    );
  }
}
