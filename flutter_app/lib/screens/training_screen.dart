import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:shared_preferences/shared_preferences.dart';

class TrainingScreen extends StatefulWidget {
  const TrainingScreen({Key? key}) : super(key: key);

  @override
  State<TrainingScreen> createState() => _TrainingScreenState();
}

class _TrainingScreenState extends State<TrainingScreen> {
  int _xp = 0;
  String _rank = "Security Trainee";
  
  // Game states:
  // 0 - Dashboard (Progress, list of scenarios)
  // 1 - Active Scenario Quiz
  // 2 - Scenario Feedback
  int _gameState = 0;
  
  int _selectedScenarioIndex = -1;
  int _selectedAnswerIndex = -1;
  bool _isAnswered = false;

  final List<Map<String, dynamic>> _scenarios = [
    {
      'title': 'The Blocked Account Smishing',
      'icon': LucideIcons.messageSquare,
      'content': "You receive an SMS from '+91-XXXXX-98761' stating:\n\n'ALERT: Your HDFC bank account is suspended due to invalid PAN registration. To re-activate your account immediately and avoid a Rs. 5000 fine, click here: https://hdfc-pan-verify.in/login'",
      'question': "What is the primary indicator that this is a smishing scam?",
      'options': [
        "The message contains a spelling mistake in PAN.",
        "It demands immediate urgency with a financial fine threat, and uses a non-official domain (hdfc-pan-verify.in).",
        "It comes from a mobile phone number instead of a bank shortcode.",
        "Both B and C."
      ],
      'correctIndex': 3,
      'explanation': "Correct! Legitimate banks will never send urgent security warnings via normal 10-digit mobile numbers (+91...) or use unofficial verification URLs. They communicate using certified alphabetic shortcodes (e.g. MD-HDFCBK) and official HTTPS portals.",
      'xp': 50
    },
    {
      'title': 'The Refund Customer Call',
      'icon': LucideIcons.phone,
      'content': "A caller claiming to be from Amazon Support says:\n\n'Sir, your payment of Rs 1,499 failed but the amount was deducted. To process your instant refund, I have sent a UPI payment request to your PhonePe app. Please open PhonePe and enter your UPI PIN to accept the refund.'",
      'question': "What should you do in this situation?",
      'options': [
        "Enter your UPI PIN immediately to retrieve your money.",
        "Decline the request and hang up. Entering your UPI PIN is only for sending money, not receiving refunds.",
        "Share your credit card details over the phone instead.",
        "Ask the caller to send the money via bank transfer instead."
      ],
      'correctIndex': 1,
      'explanation': "Correct! This is a classic UPI collect request fraud. You NEVER need to enter your UPI PIN or approve a request to *receive* money or refunds. Legitimate companies process refunds directly back to the original payment method.",
      'xp': 60
    },
    {
      'title': 'The Urgent Tax Refund Email',
      'icon': LucideIcons.mail,
      'content': "You receive an email from 'refunds@income-tax-gov-india.org' with the subject 'URGENT: Income Tax Refund Confirmation':\n\n'Dear Taxpayer, after reviewing your filing, you are eligible for an immediate tax refund of Rs 14,350. Please download the attached PDF (tax_refund_form.exe) to verify your bank details.'",
      'question': "Why is this email dangerous?",
      'options': [
        "The attachment ends with '.exe' which represents an executable file that can run malware.",
        "The email address domain ('income-tax-gov-india.org') is unofficial; government sites end in '.gov.in'.",
        "The email requests sensitive credentials via an attachment.",
        "All of the above."
      ],
      'correctIndex': 3,
      'explanation': "Correct! Government portals end strictly with '.gov.in'. Furthermore, downloading an attachment with an executable extension like '.exe' will install Trojans or spyware on your device. Never download unrecognized files from untrusted senders.",
      'xp': 70
    }
  ];

  @override
  void initState() {
    super.initState();
    _loadProgress();
  }

  Future<void> _loadProgress() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _xp = prefs.getInt('aegis_xp') ?? 0;
      _rank = _calculateRank(_xp);
    });
  }

  Future<void> _saveProgress(int newXp) async {
    final prefs = await SharedPreferences.getInstance();
    final totalXp = _xp + newXp;
    await prefs.setInt('aegis_xp', totalXp);
    final nextRank = _calculateRank(totalXp);
    await prefs.setString('aegis_rank', nextRank);
    
    setState(() {
      _xp = totalXp;
      _rank = nextRank;
    });
  }

  String _calculateRank(int xp) {
    if (xp < 100) return "Security Trainee";
    if (xp < 250) return "Phishing Detective";
    if (xp < 500) return "Cyber Specialist";
    return "Aegis Master";
  }

  void _startScenario(int index) {
    setState(() {
      _selectedScenarioIndex = index;
      _gameState = 1;
      _selectedAnswerIndex = -1;
      _isAnswered = false;
    });
  }

  void _submitAnswer(int index) {
    if (_isAnswered) return;
    setState(() {
      _selectedAnswerIndex = index;
      _isAnswered = true;
      _gameState = 2;
    });

    final scenario = _scenarios[_selectedScenarioIndex];
    if (index == scenario['correctIndex']) {
      _saveProgress(scenario['xp'] as int);
    }
  }

  void _exitScenario() {
    setState(() {
      _gameState = 0;
      _selectedScenarioIndex = -1;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_gameState == 0) return _buildDashboardView();
    if (_gameState == 1) return _buildQuizView();
    return _buildFeedbackView();
  }

  Widget _buildDashboardView() {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Security Training', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Train your instincts against phishing, smishing, and social engineering attacks.",
              style: TextStyle(color: Colors.grey, fontSize: 16),
            ),
            const SizedBox(height: 24),
            
            // Progress tracker Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF151821), Color(0xFF1F2432)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFF10B981).withOpacity(0.2)),
              ),
              child: Row(
                children: [
                  Container(
                    height: 60,
                    width: 60,
                    decoration: BoxDecoration(
                      color: const Color(0xFF10B981).withOpacity(0.1),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(LucideIcons.graduationCap, color: Color(0xFF10B981), size: 30),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(_rank, style: const TextStyle(color: Color(0xFF10B981), fontSize: 18, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 4),
                        Text("Current XP: $_xp", style: const TextStyle(color: Colors.white, fontSize: 14)),
                        const SizedBox(height: 8),
                        // Mini progress bar to next level
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: (_xp % 250) / 250,
                            backgroundColor: Colors.white10,
                            color: const Color(0xFF10B981),
                            minHeight: 6,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 32),
            const Text(
              "AVAILABLE SCENARIOS",
              style: TextStyle(color: Colors.grey, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2),
            ),
            const SizedBox(height: 16),
            
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _scenarios.length,
              itemBuilder: (context, index) {
                final scenario = _scenarios[index];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: InkWell(
                      onTap: () => _startScenario(index),
                      borderRadius: BorderRadius.circular(16),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Row(
                          children: [
                            Container(
                              height: 44,
                              width: 44,
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.04),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Icon(scenario['icon'] as IconData, color: Colors.grey),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    scenario['title'] as String,
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    "+${scenario['xp']} XP Reward",
                                    style: const TextStyle(color: Color(0xFF10B981), fontSize: 12, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                            ),
                            const Icon(Icons.arrow_forward_ios, size: 16, color: Colors.white24),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuizView() {
    final scenario = _scenarios[_selectedScenarioIndex];
    return Scaffold(
      appBar: AppBar(
        title: Text(scenario['title'] as String, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: _exitScenario,
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Threat Scenario Context Card
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.white10),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(LucideIcons.shieldAlert, color: Colors.orange, size: 18),
                      SizedBox(width: 8),
                      Text("SCENARIO INVESTIGATION", style: TextStyle(color: Colors.orange, fontWeight: FontWeight.bold, fontSize: 12, letterSpacing: 1)),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    scenario['content'] as String,
                    style: const TextStyle(color: Colors.white, fontSize: 14, height: 1.6, fontFamily: 'monospace'),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 28),
            Text(
              scenario['question'] as String,
              style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            
            // Multiple Choice Options List
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: (scenario['options'] as List).length,
              itemBuilder: (context, index) {
                final optionText = (scenario['options'] as List)[index];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: InkWell(
                    onTap: () => _submitAnswer(index),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.03),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.white10),
                      ),
                      child: Text(
                        optionText,
                        style: const TextStyle(color: Colors.white70, fontSize: 14, height: 1.4),
                      ),
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFeedbackView() {
    final scenario = _scenarios[_selectedScenarioIndex];
    final isCorrect = _selectedAnswerIndex == scenario['correctIndex'];
    final accentColor = isCorrect ? const Color(0xFF10B981) : Colors.red;
    
    return Scaffold(
      appBar: AppBar(
        title: const Text("Evaluation Result", style: TextStyle(fontWeight: FontWeight.bold)),
        automaticallyImplyLeading: false,
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                isCorrect ? LucideIcons.checkCircle : LucideIcons.xCircle,
                size: 80,
                color: accentColor,
              ),
              const SizedBox(height: 24),
              Text(
                isCorrect ? "Correct Choice!" : "Incorrect Choice",
                style: TextStyle(color: accentColor, fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                isCorrect ? "+${scenario['xp']} XP Earned" : "+0 XP Earned",
                style: const TextStyle(color: Colors.grey, fontSize: 16),
              ),
              const SizedBox(height: 32),
              
              // Educational explanation card
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Theme.of(context).cardColor,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: Colors.white10),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      "WHY?",
                      style: TextStyle(color: accentColor, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      scenario['explanation'] as String,
                      style: const TextStyle(color: Colors.white70, fontSize: 14, height: 1.5),
                    ),
                  ],
                ),
              ),
              
              const SizedBox(height: 48),
              ElevatedButton(
                onPressed: _exitScenario,
                style: ElevatedButton.styleFrom(
                  backgroundColor: isCorrect ? const Color(0xFF10B981) : Colors.white10,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 48, vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                ),
                child: const Text("Continue Training", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
