import 'package:flutter/material.dart';
import 'screens/main_layout.dart';

void main() {
  runApp(const AegisApp());
}

class AegisApp extends StatelessWidget {
  const AegisApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Aegis AI',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0B0D12),
        primaryColor: const Color(0xFF10B981),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF10B981),
          surface: Color(0xFF151821),
        ),
        cardColor: const Color(0xFF151821),
        fontFamily: 'Inter', // Note: You'll need to add Inter font to pubspec.yaml if you want exact typography
      ),
      home: const MainLayout(),
    );
  }
}
