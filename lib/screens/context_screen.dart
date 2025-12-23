import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../providers/context_provider.dart';

class ContextScreen extends StatefulWidget {
  const ContextScreen({super.key});

  @override
  State<ContextScreen> createState() => _ContextScreenState();
}

class _ContextScreenState extends State<ContextScreen> {
  final TextEditingController _profileController = TextEditingController();
  XFile? _selectedImage;

  Future<void> _pickImage() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: ImageSource.gallery);
    setState(() {
      _selectedImage = image;
    });
  }

  Future<void> _saveContext() async {
    if (_selectedImage == null || _profileController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select an image and enter a profile name')),
      );
      return;
    }

    // Mock OCR: generate a sample context based on profile name
    String contextSummary = 'Relationship with ${_profileController.text}: Casual and friendly interactions.';
    String contextId = _profileController.text.toLowerCase().replaceAll(' ', '_');

    // Save to SharedPreferences
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('context_$contextId', contextSummary);
    await prefs.setString('profile_$contextId', _profileController.text);

    // Update provider
    Provider.of<ContextProvider>(context, listen: false).addContext(contextId, _profileController.text);

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Context saved successfully')),
    );

    _profileController.clear();
    setState(() {
      _selectedImage = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final contexts = Provider.of<ContextProvider>(context).contexts;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Context Manager'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            ElevatedButton(
              onPressed: _pickImage,
              child: const Text('Upload Chat Screenshot'),
            ),
            if (_selectedImage != null)
              Image.file(
                File(_selectedImage!.path),
                height: 200,
                width: 200,
                fit: BoxFit.cover,
              ),
            TextField(
              controller: _profileController,
              decoration: const InputDecoration(labelText: 'Profile Name (e.g., Sarah)'),
            ),
            ElevatedButton(
              onPressed: _saveContext,
              child: const Text('Save Context'),
            ),
            const SizedBox(height: 20),
            const Text('Saved Contexts:', style: TextStyle(fontSize: 18)),
            Expanded(
              child: ListView.builder(
                itemCount: contexts.length,
                itemBuilder: (context, index) {
                  final entry = contexts.entries.elementAt(index);
                  return ListTile(
                    title: Text(entry.value),
                    subtitle: Text('ID: ${entry.key}'),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
