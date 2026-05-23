import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:http/http.dart' as http;
import '../utils/api_config.dart';

class VishingScreen extends StatefulWidget {
  const VishingScreen({Key? key}) : super(key: key);

  @override
  State<VishingScreen> createState() => _VishingScreenState();
}

class _VishingScreenState extends State<VishingScreen> with TickerProviderStateMixin {
  // Simulator States:
  // 0 - Idle (Start monitoring button)
  // 1 - Listening/Radar (Scanning for incoming calls)
  // 2 - Ringing (Incoming call overlay)
  // 3 - In Call (Ongoing transcription and real-time scanning)
  // 4 - Call Ended Summary
  int _simState = 0;

  Timer? _simulationTimer;
  Timer? _callTimer;
  int _secondsElapsed = 0;
  
  // Call Details
  final String _callerName = "Suspected Telemarketing";
  final String _callerNumber = "+91 98765 43210";
  
  // Script dialogue
  final List<String> _dialogueScript = [
    "Hello? Kya meri baat Mr. Amit se ho rahi hai?",
    "Ji, main SBI Card Verification Department se bol raha hoon. Aapka credit card high-risk alert par hai aur use temporary block kiya gaya hai.",
    "Aapko card unblock karne ke liye, verify karna hoga. Main aapse password nahi poochunga, bas aapki details confirm karni hain.",
    "Humne aapke mobile par ek verification code ya OTP bheja hai. Kripya use jaldi bataiye taaki card unblock ho sake, nahi toh card verify nahi hoga.",
  ];

  int _currentDialogueIndex = 0;
  final List<Map<String, dynamic>> _callTranscript = []; // List of dialogue utterances
  final ScrollController _scrollController = ScrollController();
  
  // API Threat evaluation results
  String _riskLevel = "SAFE";
  double _confidence = 0.0;
  String _reason = "Listening for conversation clues...";
  bool _isScamAlert = false;
  bool _isQueryingApi = false;

  late AnimationController _radarController;
  late AnimationController _pulseController;
  late AnimationController _alertController;

  @override
  void initState() {
    super.initState();
    _radarController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    );
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );
    _alertController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
  }

  @override
  void dispose() {
    _simulationTimer?.cancel();
    _callTimer?.cancel();
    _radarController.dispose();
    _pulseController.dispose();
    _alertController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _startRadar() {
    setState(() {
      _simState = 1;
    });
    _radarController.repeat();
    _pulseController.repeat(reverse: true);

    // Call rings after 3.5 seconds
    _simulationTimer = Timer(const Duration(milliseconds: 3500), () {
      _radarController.stop();
      if (mounted) {
        setState(() {
          _simState = 2;
        });
      }
    });
  }

  void _declineCall() {
    _simulationTimer?.cancel();
    _callTimer?.cancel();
    setState(() {
      _simState = 0;
    });
  }

  void _acceptCall() {
    setState(() {
      _simState = 3;
      _secondsElapsed = 0;
      _currentDialogueIndex = 0;
      _callTranscript.clear();
      _riskLevel = "SAFE";
      _confidence = 0.05;
      _reason = "Call established. Listening for conversation keywords...";
      _isScamAlert = false;
    });

    // Start Call Timer
    _callTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (mounted) {
        setState(() {
          _secondsElapsed++;
        });
      }
    });

    // Simulate caller starting to speak after 1.5 seconds, then every 6 seconds
    _triggerNextDialogue();
  }

  void _triggerNextDialogue() {
    _simulationTimer = Timer(const Duration(milliseconds: 2000), () async {
      if (_currentDialogueIndex >= _dialogueScript.length || _simState != 3) {
        return;
      }

      final text = _dialogueScript[_currentDialogueIndex];
      if (mounted) {
        setState(() {
          _callTranscript.add({
            'sender': 'caller',
            'text': text,
            'timestamp': DateTime.now()
          });
          _currentDialogueIndex++;
        });
        
        // Auto Scroll to bottom
        _scrollToBottom();

        // Perform real-time transcript scan
        await _evaluateTranscript();
      }

      // Schedule next dialogue
      if (_currentDialogueIndex < _dialogueScript.length && _simState == 3) {
        _triggerNextDialogue();
      }
    });
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _evaluateTranscript() async {
    if (_callTranscript.isEmpty) return;

    // Accumulate entire transcript
    final fullText = _callTranscript.map((t) => t['text'] as String).join(" ");
    
    if (mounted) {
      setState(() {
        _isQueryingApi = true;
      });
    }

    try {
      final baseUrl = await ApiConfig.getBaseUrl();
      final response = await http.post(
        Uri.parse('$baseUrl/api/vishing/analyze-transcript'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'transcript': fullText,
          'phone_number': _callerNumber
        }),
      );

      if (response.statusCode == 200 && mounted) {
        final data = jsonDecode(response.body);
        setState(() {
          _riskLevel = data['risk'] ?? 'UNKNOWN';
          _confidence = (data['confidence'] ?? 0.0).toDouble();
          _reason = data['reason'] ?? 'No reason provided';
          _isScamAlert = _riskLevel == 'SCAM';
        });
        if (_isScamAlert) {
          _alertController.repeat(reverse: true);
        } else {
          _alertController.stop();
        }
      } else {
        throw Exception();
      }
    } catch (_) {
      // Local evaluation fallback
      _evaluateOffline(fullText);
    } finally {
      if (mounted) {
        setState(() {
          _isQueryingApi = false;
        });
      }
    }
  }

  void _evaluateOffline(String text) {
    final lower = text.toLowerCase();
    String newRisk = "SAFE";
    double newConf = 0.1;
    String newReason = "No threat signatures identified in conversation transcript.";

    // Simple keyword matches
    final matches = <String>[];
    if (lower.contains("sbi") || lower.contains("bank") || lower.contains("credit card")) {
      matches.add("Bank Impersonation");
    }
    if (lower.contains("block")) {
      matches.add("Threat (Block account)");
    }
    if (lower.contains("verify") || lower.contains("details")) {
      matches.add("Identity Request");
    }
    if (lower.contains("otp") || lower.contains("code") || lower.contains("code")) {
      matches.add("OTP Harvesting Request");
    }

    if (matches.length >= 3 || lower.contains("otp")) {
      newRisk = "SCAM";
      newConf = 0.92;
      newReason = "CRITICAL: Caller requested a verification OTP. This is a primary vishing signature. (Offline Evaluator)";
    } else if (matches.isNotEmpty) {
      newRisk = "SUSPICIOUS";
      newConf = 0.65;
      newReason = "WARNING: Caller claiming to be from Bank Department requesting card details. High warning indicators. (Offline Evaluator)";
    }

    if (mounted) {
      setState(() {
        _riskLevel = newRisk;
        _confidence = newConf;
        _reason = newReason;
        _isScamAlert = _riskLevel == 'SCAM';
      });
      if (_isScamAlert) {
        _alertController.repeat(reverse: true);
      } else {
        _alertController.stop();
      }
    }
  }

  void _hangUp() {
    _simulationTimer?.cancel();
    _callTimer?.cancel();
    _alertController.stop();
    setState(() {
      _simState = 4;
    });
  }

  void _resetSimulation() {
    _simulationTimer?.cancel();
    _callTimer?.cancel();
    _alertController.stop();
    setState(() {
      _simState = 0;
      _callTranscript.clear();
      _currentDialogueIndex = 0;
      _secondsElapsed = 0;
    });
  }

  String _formatTimer(int totalSeconds) {
    int minutes = totalSeconds ~/ 60;
    int seconds = totalSeconds % 60;
    return "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
  }

  @override
  Widget build(BuildContext context) {
    if (_simState == 0) return _buildIdleView();
    if (_simState == 1) return _buildRadarView();
    if (_simState == 2) return _buildIncomingCallOverlay();
    if (_simState == 3) return _buildInCallView();
    return _buildSummaryView();
  }

  Widget _buildIdleView() {
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
            const Text('Vishing Guardian', style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.record_voice_over, size: 80, color: Theme.of(context).primaryColor.withOpacity(0.5)),
              const SizedBox(height: 24),
              const Text("AI Call Guardian", style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 24),
                child: Text(
                  "Intercept, transcribe, and analyze active phone conversations for social engineering vishing signs in real-time.",
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey, fontSize: 15, height: 1.5),
                ),
              ),
              const SizedBox(height: 40),
              ElevatedButton.icon(
                onPressed: _startRadar,
                icon: const Icon(LucideIcons.shieldAlert),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).primaryColor,
                  padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                  elevation: 6,
                ),
                label: const Text("Start Call Monitoring", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
              ),
              const SizedBox(height: 16),
              const Text("Will monitor in the background when phone calls connect.", style: TextStyle(color: Colors.white30, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRadarView() {
    return Scaffold(
      backgroundColor: const Color(0xFF07090C),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedBuilder(
              animation: _pulseController,
              builder: (context, child) {
                return Container(
                  height: 180,
                  width: 180,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: const Color(0xFF10B981).withOpacity(0.05 + (_pulseController.value * 0.1)),
                    border: Border.all(
                      color: const Color(0xFF10B981).withOpacity(0.2 + (_pulseController.value * 0.4)),
                      width: 1 + (_pulseController.value * 3),
                    ),
                  ),
                  child: Center(
                    child: RotationTransition(
                      turns: _radarController,
                      child: Container(
                        height: 100,
                        width: 100,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: SweepGradient(
                            colors: [
                              const Color(0xFF10B981).withOpacity(0.0),
                              const Color(0xFF10B981).withOpacity(0.4),
                              const Color(0xFF10B981).withOpacity(0.8),
                            ],
                            stops: const [0.0, 0.7, 1.0],
                          ),
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 48),
            const Text(
              "Listening for Calls...",
              style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold, letterSpacing: 1.1),
            ),
            const SizedBox(height: 12),
            const Text(
              "Aegis Shield active. Standing by for incoming calls...",
              style: TextStyle(color: Colors.grey, fontSize: 14),
            ),
            const SizedBox(height: 40),
            TextButton(
              onPressed: _declineCall,
              child: const Text("Cancel Monitoring", style: TextStyle(color: Colors.redAccent, fontSize: 15)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildIncomingCallOverlay() {
    return Scaffold(
      backgroundColor: const Color(0xFF0F121A),
      body: SafeArea(
        child: Column(
          children: [
            const Spacer(),
            CircleAvatar(
              radius: 48,
              backgroundColor: Colors.white12,
              child: Icon(LucideIcons.user, size: 48, color: Colors.white.withOpacity(0.6)),
            ),
            const SizedBox(height: 24),
            Text(
              _callerName,
              style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              _callerNumber,
              style: const TextStyle(color: Colors.grey, fontSize: 18),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.orange.withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: Colors.orange.withOpacity(0.3)),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(LucideIcons.shieldAlert, size: 14, color: Colors.orange),
                  SizedBox(width: 6),
                  Text("SUSPECTED SPAM CALL", style: TextStyle(color: Colors.orange, fontSize: 12, fontWeight: FontWeight.bold)),
                ],
              ),
            ),
            const Spacer(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 48, vertical: 48),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    children: [
                      FloatingActionButton(
                        heroTag: 'decline_btn',
                        onPressed: _declineCall,
                        backgroundColor: Colors.red,
                        child: const Icon(LucideIcons.phoneOff, color: Colors.white),
                      ),
                      const SizedBox(height: 8),
                      const Text("Decline", style: TextStyle(color: Colors.grey, fontSize: 14)),
                    ],
                  ),
                  Column(
                    children: [
                      FloatingActionButton(
                        heroTag: 'accept_btn',
                        onPressed: _acceptCall,
                        backgroundColor: const Color(0xFF10B981),
                        child: const Icon(LucideIcons.phoneCall, color: Colors.white),
                      ),
                      const SizedBox(height: 8),
                      const Text("Accept & Monitor", style: TextStyle(color: Colors.grey, fontSize: 14)),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInCallView() {
    final riskColor = _riskLevel == 'SAFE' 
        ? const Color(0xFF10B981) 
        : (_riskLevel == 'SCAM' ? Colors.red : Colors.orange);
    
    return Scaffold(
      backgroundColor: const Color(0xFF0F121A),
      body: SafeArea(
        child: Column(
          children: [
            // Alert Flashing Banner
            if (_isScamAlert)
              AnimatedBuilder(
                animation: _alertController,
                builder: (context, child) {
                  return Container(
                    width: double.infinity,
                    color: Colors.red.withOpacity(0.3 + (_alertController.value * 0.7)),
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    alignment: Alignment.center,
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(LucideIcons.alertTriangle, color: Colors.white),
                        SizedBox(width: 8),
                        Text(
                          "DANGER: SCAM DETECTED! HANG UP IMMEDIATELY!",
                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                        ),
                      ],
                    ),
                  );
                },
              ),
            
            // Header stats
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  CircleAvatar(
                    backgroundColor: Colors.white12,
                    child: Icon(LucideIcons.user, color: Colors.white.withOpacity(0.6)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(_callerName, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                        Text("Monitoring Call • ${_formatTimer(_secondsElapsed)}", style: const TextStyle(color: Colors.grey, fontSize: 12)),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: riskColor.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: riskColor.withOpacity(0.5)),
                    ),
                    child: Row(
                      children: [
                        if (_isQueryingApi) ...[
                          const SizedBox(
                            width: 10,
                            height: 10,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          ),
                          const SizedBox(width: 6),
                        ],
                        Text(
                          _riskLevel,
                          style: TextStyle(color: riskColor, fontWeight: FontWeight.bold, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            
            // Threat Reasoning Card
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.03),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.white10),
                ),
                child: Row(
                  children: [
                    Icon(
                      _riskLevel == 'SAFE' ? LucideIcons.checkCircle : (_riskLevel == 'SCAM' ? LucideIcons.shieldAlert : LucideIcons.alertTriangle),
                      color: riskColor,
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text("Reason: $_reason", style: const TextStyle(color: Colors.white70, fontSize: 12, height: 1.4)),
                          if (_confidence > 0)
                            Padding(
                              padding: const EdgeInsets.only(top: 4),
                              child: Text("Confidence: ${(_confidence * 100).round()}%", style: TextStyle(color: riskColor.withOpacity(0.8), fontSize: 10, fontWeight: FontWeight.bold)),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // Dialogue transcript area
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.black.withOpacity(0.25),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: Colors.white10),
                  ),
                  child: _callTranscript.isEmpty
                      ? const Center(child: Text("Waiting for caller to speak...", style: TextStyle(color: Colors.grey, fontSize: 14)))
                      : ListView.builder(
                          controller: _scrollController,
                          itemCount: _callTranscript.length,
                          itemBuilder: (context, index) {
                            final chat = _callTranscript[index];
                            return Padding(
                              padding: const EdgeInsets.symmetric(vertical: 8),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  CircleAvatar(
                                    radius: 14,
                                    backgroundColor: Colors.red.withOpacity(0.2),
                                    child: const Icon(LucideIcons.phoneOutgoing, size: 12, color: Colors.redAccent),
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Container(
                                      padding: const EdgeInsets.all(12),
                                      decoration: BoxDecoration(
                                        color: Colors.white.withOpacity(0.05),
                                        borderRadius: const BorderRadius.only(
                                          topRight: Radius.circular(12),
                                          bottomLeft: Radius.circular(12),
                                          bottomRight: Radius.circular(12),
                                        ),
                                      ),
                                      child: Text(
                                        chat['text']!,
                                        style: const TextStyle(color: Colors.white, fontSize: 14, height: 1.4),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                ),
              ),
            ),

            // Visualizer & Call controls
            Padding(
              padding: const EdgeInsets.only(bottom: 24, left: 16, right: 16),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(5, (index) {
                      return Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: AnimatedBuilder(
                          animation: _pulseController,
                          builder: (context, child) {
                            double scale = 1.0;
                            if (index == 2) scale = 1.5;
                            if (index == 1 || index == 3) scale = 1.2;
                            return Container(
                              height: 16 * scale * (_pulseController.value + 0.3),
                              width: 6,
                              decoration: BoxDecoration(
                                color: riskColor.withOpacity(0.7),
                                borderRadius: BorderRadius.circular(4),
                              ),
                            );
                          },
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      ElevatedButton.icon(
                        onPressed: _hangUp,
                        icon: const Icon(LucideIcons.phoneOff, size: 18),
                        label: const Text("Hang Up", style: TextStyle(fontWeight: FontWeight.bold)),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.red,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                      if (_riskLevel == 'SCAM')
                        ElevatedButton.icon(
                          onPressed: _hangUp,
                          icon: const Icon(LucideIcons.shieldAlert, size: 18),
                          label: const Text("Block & Drop", style: TextStyle(fontWeight: FontWeight.bold)),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.redAccent,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryView() {
    final isScam = _riskLevel == 'SCAM';
    
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
            const Text('Call Summary', style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        backgroundColor: Colors.transparent,
        automaticallyImplyLeading: false,
        elevation: 0,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                isScam ? LucideIcons.shieldAlert : LucideIcons.checkCircle,
                size: 80,
                color: isScam ? Colors.red : const Color(0xFF10B981),
              ),
              const SizedBox(height: 24),
              Text(
                isScam ? "Scam Attempt Flagged!" : "Call Terminated Safely",
                style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              Text(
                "Phone: $_callerNumber",
                style: const TextStyle(color: Colors.grey, fontSize: 16),
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
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text("FINAL EVALUATION", style: TextStyle(color: Colors.grey, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2)),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Text("Risk Level: ", style: const TextStyle(color: Colors.white70)),
                        Text(_riskLevel, style: TextStyle(color: isScam ? Colors.red : const Color(0xFF10B981), fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text("Reasoning: $_reason", style: const TextStyle(color: Colors.white60, fontSize: 13, height: 1.4)),
                  ],
                ),
              ),
              const SizedBox(height: 40),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  OutlinedButton(
                    onPressed: _resetSimulation,
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                      side: const BorderSide(color: Colors.white24),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: const Text("Done", style: TextStyle(color: Colors.white)),
                  ),
                  if (isScam)
                    ElevatedButton.icon(
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text("Caller logged in local database and reported to national cybercrime division.")),
                        );
                        _resetSimulation();
                      },
                      icon: const Icon(LucideIcons.shield),
                      label: const Text("Report Number"),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.red,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
