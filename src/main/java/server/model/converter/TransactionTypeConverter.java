package server.model.converter;

import common.enums.TransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

  @Override
  public String convertToDatabaseColumn(TransactionType type) {
    return type == null ? null : type.name();
  }

  @Override
  public TransactionType convertToEntityAttribute(String value) {
    return value == null ? null : TransactionType.fromValue(value);
  }
}
