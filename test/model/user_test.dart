
import 'package:app_test_cicd/model/user.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('User JSON serialization', () {
    final json = {'id': '42', 'name': 'Ana', 'email': 'ana@example.com'};
    final user = User(id: '42', name: 'Ana', email: 'ana@example.com');

    test('fromJson crea correctamente un User', () {
      final parsed = User.fromJson(json);
      expect(parsed.id, user.id);
      expect(parsed.name, user.name);
      expect(parsed.email, user.email);
    });

    test('toJson retorna el mapa correcto', () {
      expect(user.toJson(), json);
    });
  });
}