import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({Key? key}) : super(key: key);

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  String _username = "Guest";
  String _email = "";
  String _phone = "";
  String _userId = "";
  String _rank = "Security Trainee";
  int _xp = 0;

  @override
  void initState() {
    super.initState();
    _loadProfileData();
  }

  Future<void> _loadProfileData() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _username = prefs.getString('aegis_user_name') ?? "Guest";
      _email = prefs.getString('aegis_user_email') ?? "";
      _phone = prefs.getString('aegis_user_phone') ?? "";
      _userId = prefs.getString('aegis_user_id') ?? _generateUserId();
      _xp = prefs.getInt('aegis_xp') ?? 0;
      _rank = _calculateRank(_xp);
    });

    // Save generated user ID if it was just created
    if ((prefs.getString('aegis_user_id') ?? "").isEmpty) {
      await prefs.setString('aegis_user_id', _userId);
    }
  }

  String _generateUserId() {
    final now = DateTime.now();
    return "${now.year}${now.month.toString().padLeft(2, '0')}${now.day.toString().padLeft(2, '0')}${now.millisecond}";
  }

  String _calculateRank(int xp) {
    if (xp < 100) return "Security Trainee";
    if (xp < 250) return "Phishing Detective";
    if (xp < 500) return "Cyber Specialist";
    return "Aegis Master";
  }

  Future<void> _saveField(String key, String value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(key, value.trim());
  }

  void _showEditDialog({
    required String title,
    required String currentValue,
    required String hint,
    required String prefKey,
    required Function(String) onSaved,
    TextInputType keyboardType = TextInputType.text,
  }) {
    final controller = TextEditingController(text: currentValue);
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(0xFF151821),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 18)),
          content: TextField(
            controller: controller,
            keyboardType: keyboardType,
            style: const TextStyle(color: Colors.white),
            autofocus: true,
            decoration: InputDecoration(
              hintText: hint,
              hintStyle: const TextStyle(color: Colors.grey),
              enabledBorder: const UnderlineInputBorder(borderSide: BorderSide(color: Colors.white24)),
              focusedBorder: const UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFF10B981))),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel', style: TextStyle(color: Colors.grey)),
            ),
            TextButton(
              onPressed: () {
                final newVal = controller.text.trim();
                if (newVal.isNotEmpty) {
                  _saveField(prefKey, newVal);
                  onSaved(newVal);
                }
                Navigator.pop(context);
              },
              child: const Text('Save', style: TextStyle(color: Color(0xFF10B981), fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }

  void _showEditProfileSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF151821),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      isScrollControlled: true,
      builder: (context) {
        return Padding(
          padding: EdgeInsets.fromLTRB(20, 24, 20, MediaQuery.of(context).viewInsets.bottom + 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    "Edit Profile",
                    style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.close, color: Colors.grey),
                  ),
                ],
              ),
              const Divider(color: Colors.white10),
              const SizedBox(height: 8),
              _editOptionTile(
                icon: LucideIcons.user,
                label: "Username",
                value: _username,
                onTap: () {
                  Navigator.pop(context);
                  _showEditDialog(
                    title: "Edit Username",
                    currentValue: _username,
                    hint: "Enter your username",
                    prefKey: 'aegis_user_name',
                    onSaved: (v) => setState(() => _username = v),
                  );
                },
              ),
              _editOptionTile(
                icon: LucideIcons.mail,
                label: "Email",
                value: _email.isEmpty ? "Not set" : _email,
                onTap: () {
                  Navigator.pop(context);
                  _showEditDialog(
                    title: "Edit Email",
                    currentValue: _email,
                    hint: "Enter your email address",
                    prefKey: 'aegis_user_email',
                    keyboardType: TextInputType.emailAddress,
                    onSaved: (v) => setState(() => _email = v),
                  );
                },
              ),
              _editOptionTile(
                icon: LucideIcons.phone,
                label: "Phone",
                value: _phone.isEmpty ? "Not set" : _phone,
                onTap: () {
                  Navigator.pop(context);
                  _showEditDialog(
                    title: "Edit Phone Number",
                    currentValue: _phone,
                    hint: "Enter your phone number",
                    prefKey: 'aegis_user_phone',
                    keyboardType: TextInputType.phone,
                    onSaved: (v) => setState(() => _phone = v),
                  );
                },
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
  }

  Widget _editOptionTile({
    required IconData icon,
    required String label,
    required String value,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 14),
        child: Row(
          children: [
            Icon(icon, color: const Color(0xFF10B981), size: 20),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
                  const SizedBox(height: 2),
                  Text(value, style: const TextStyle(color: Colors.white, fontSize: 15)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: Colors.white24, size: 20),
          ],
        ),
      ),
    );
  }

  void _confirmLogout() {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(0xFF151821),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text("Log out", style: TextStyle(color: Colors.white)),
          content: const Text(
            "Are you sure you want to log out? Your local profile data will be cleared.",
            style: TextStyle(color: Colors.grey, height: 1.4),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Cancel", style: TextStyle(color: Colors.grey)),
            ),
            TextButton(
              onPressed: () async {
                final prefs = await SharedPreferences.getInstance();
                await prefs.clear();
                if (mounted) {
                  Navigator.pop(context);
                  setState(() {
                    _username = "Guest";
                    _email = "";
                    _phone = "";
                    _xp = 0;
                    _rank = "Security Trainee";
                  });
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text("Logged out. All local data cleared.")),
                  );
                }
              },
              child: const Text("Log out", style: TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 60, 16, 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Hi $_username!",
              style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              "Your personal security and awareness overview.",
              style: TextStyle(color: Colors.grey, fontSize: 15),
            ),
            const SizedBox(height: 24),

            // Profile avatar card
            _buildProfileCard(),
            const SizedBox(height: 16),

            // Training rank card
            _buildRankCard(),
            const SizedBox(height: 16),

            // Personal information card
            _buildPersonalInfoCard(),
            const SizedBox(height: 24),

            // Edit Profile button
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: _showEditProfileSheet,
                icon: const Icon(LucideIcons.edit2, size: 16),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  side: const BorderSide(color: Color(0xFF10B981)),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                label: const Text("Edit Profile", style: TextStyle(color: Color(0xFF10B981), fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ),
            const SizedBox(height: 12),

            // Log out button
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: _confirmLogout,
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  side: const BorderSide(color: Colors.redAccent),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text("Log out", style: TextStyle(color: Colors.redAccent, fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white10),
      ),
      child: Column(
        children: [
          CircleAvatar(
            radius: 40,
            backgroundColor: const Color(0xFF10B981),
            child: Text(
              _username.length >= 2 ? _username.substring(0, 2).toUpperCase() : "G",
              style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: Colors.white),
            ),
          ),
          const SizedBox(height: 16),
          Text(_username, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white)),
          const SizedBox(height: 4),
          Text(
            _email.isNotEmpty ? _email : "No email linked",
            style: const TextStyle(color: Colors.grey),
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  Widget _buildRankCard() {
    return Container(
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
          const Text("Training rank", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Text(
            "$_rank · $_xp XP",
            style: const TextStyle(color: Color(0xFF10B981), fontSize: 16, fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }

  Widget _buildPersonalInfoCard() {
    return Container(
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
          const Text("Personal information", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 20),
          _infoField(label: "User ID", value: _userId),
          const SizedBox(height: 16),
          _infoField(label: "Email", value: _email.isNotEmpty ? _email : "Not set"),
          const SizedBox(height: 16),
          _infoField(label: "Phone", value: _phone.isNotEmpty ? _phone : "Not set"),
        ],
      ),
    );
  }

  Widget _infoField({required String label, required String value}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.04),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white10),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
          const SizedBox(height: 4),
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 15)),
        ],
      ),
    );
  }
}
