package org.ouracademy.srtomy.directory.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.directory.query.DirectoryReader.Audience;
import org.ouracademy.srtomy.directory.repository.DirectoryRepository;
import org.ouracademy.srtomy.directory.repository.DirectoryRepository.HeldRow;
import org.ouracademy.srtomy.directory.repository.DirectoryRepository.PersonRow;
import org.ouracademy.srtomy.party.domain.PartyId;

class DirectoryReaderTests {

  DirectoryRepository repository = mock(DirectoryRepository.class);
  DirectoryReader reader = new DirectoryReader(repository);

  PartyId node = PartyId.generate();
  LocalDate adult = LocalDate.now().minusYears(30);
  LocalDate minor = LocalDate.now().minusYears(10);

  PersonRow row(String first, Integer age, LocalDate birthdate) {
    return new PersonRow(node, PartyId.generate(), first, "Prueba", age, birthdate);
  }

  @Test
  void insideTheChurchOnlyKnownMinorsAreLeftOut() {
    when(repository.findPeople(List.of(node), IS_PART_OF))
        .thenReturn(
            List.of(row("Ana", null, adult), row("Beto", null, null), row("Cleo", null, minor)));

    assertThat(reader.membersOf(node, Audience.CHURCH))
        .containsExactly("Ana Prueba", "Beto Prueba");
  }

  @Test
  void aPublicPageNamesOnlyPeopleKnownToBeAdults() {
    when(repository.findPeople(List.of(node), IS_PART_OF))
        .thenReturn(
            List.of(
                row("Ana", null, adult),
                row("Beto", null, null),
                row("Cleo", null, minor),
                row("Dan", 40, null),
                row("Eva", 15, null)));

    assertThat(reader.membersOf(node, Audience.PUBLIC)).containsExactly("Ana Prueba", "Dan Prueba");
  }

  @Test
  void leadersAreGroupedByNodeAndFilteredLikeMembers() {
    var other = PartyId.generate();
    var otherLeader = new PersonRow(other, PartyId.generate(), "Lia", "Paz", null, adult);
    when(repository.findPeople(List.of(node, other), LEADER))
        .thenReturn(List.of(row("Ana", null, adult), row("Cleo", null, minor), otherLeader));

    var leaders = reader.leadersByNode(List.of(node, other), Audience.PUBLIC);

    assertThat(leaders.get(node)).containsExactly("Ana Prueba");
    assertThat(leaders.get(other)).containsExactly("Lia Paz");
  }

  @Test
  void positionsListWhoHoldsEachOne() {
    when(repository.findHeldPositions(List.of(node)))
        .thenReturn(
            List.of(
                new HeldRow(node, "Pastor", PartyId.generate(), "Ana", "Ruiz", null, adult),
                new HeldRow(node, "Pastor", PartyId.generate(), "Beto", "Paz", null, adult),
                new HeldRow(node, "Pastor", PartyId.generate(), "Menor", "X", null, minor),
                new HeldRow(node, "Diácono", PartyId.generate(), "Eva", "Sol", null, adult)));

    var positions = reader.positionsOf(node, Audience.PUBLIC);

    assertThat(positions).containsOnlyKeys("Pastor", "Diácono");
    assertThat(positions.get("Pastor")).containsExactly("Ana Ruiz", "Beto Paz");
  }

  @Test
  void aMissingNameIsNotPrintedAsNull() {
    assertThat(DirectoryReader.fullName(null, "Paz")).isEqualTo("Paz");
    assertThat(DirectoryReader.fullName("Ana", null)).isEqualTo("Ana");
  }
}
