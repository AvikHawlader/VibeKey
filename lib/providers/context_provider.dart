import 'package:flutter/material.dart';

class ContextProvider with ChangeNotifier {
  final Map<String, String> _contexts = {};

  Map<String, String> get contexts => _contexts;

  void addContext(String id, String name) {
    _contexts[id] = name;
    notifyListeners();
  }

  void loadContexts() {
    // Load from SharedPreferences if needed, but for now, assume loaded elsewhere
    notifyListeners();
  }
}
