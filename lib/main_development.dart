import 'dart:developer';

import 'package:flutter/material.dart';

void main() {
  log('PRODUCCION');
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Material App',
      home: Scaffold(
        appBar: AppBar(title: const Text('DEVELOPMENT')),
        body: const Center(child: Text('Hello World')),
      ),
    );
  }
}
