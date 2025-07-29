import 'package:app_test_cicd/utils/validators.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Email validator', () {
    test('retorna true para emails válidos', () {
      expect(isValidEmail('juan.perez@mail.com'), isTrue);
      expect(isValidEmail('a@b.co'), isTrue);
    });

    test('retorna false para emails inválidos o nulos', () {
      expect(isValidEmail('sin-arroba.com'), isFalse);
      expect(isValidEmail(''), isFalse);
      expect(isValidEmail(null), isFalse);
    });
  });
}
