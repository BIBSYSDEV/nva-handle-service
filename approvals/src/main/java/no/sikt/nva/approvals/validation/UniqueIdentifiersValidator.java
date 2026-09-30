package no.sikt.nva.approvals.validation;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import no.sikt.nva.approvals.domain.NamedIdentifier;

public class UniqueIdentifiersValidator
    implements ConstraintValidator<UniqueIdentifiers, Collection<?>> {

  @Override
  public boolean isValid(Collection<?> namedIdentifiers, ConstraintValidatorContext context) {
    if (isNull(namedIdentifiers)) {
      return true;
    }
    var duplicateIndexes = findDuplicateIndexes(new ArrayList<>(namedIdentifiers));
    if (duplicateIndexes.isEmpty()) {
      return true;
    }
    reportDuplicates(duplicateIndexes, context);
    return false;
  }

  private static List<Integer> findDuplicateIndexes(List<?> namedIdentifiers) {
    var seenKeys = new HashSet<List<String>>();
    return IntStream.range(0, namedIdentifiers.size())
        .filter(index -> isComparable(namedIdentifiers.get(index)))
        .filter(index -> !seenKeys.add(duplicateDetectionKey(namedIdentifiers.get(index))))
        .boxed()
        .toList();
  }

  private static boolean isComparable(Object element) {
    return element instanceof NamedIdentifier namedIdentifier
        && nonNull(namedIdentifier.name())
        && nonNull(namedIdentifier.value());
  }

  private static List<String> duplicateDetectionKey(Object element) {
    var namedIdentifier = (NamedIdentifier) element;
    return List.of(namedIdentifier.normalizedName(), namedIdentifier.value());
  }

  private static void reportDuplicates(
      Collection<Integer> duplicateIndexes, ConstraintValidatorContext context) {
    var messageTemplate = context.getDefaultConstraintMessageTemplate();
    context.disableDefaultConstraintViolation();
    duplicateIndexes.forEach(
        index ->
            context
                .buildConstraintViolationWithTemplate(messageTemplate)
                .addBeanNode()
                .inIterable()
                .atIndex(index)
                .addConstraintViolation());
  }
}
