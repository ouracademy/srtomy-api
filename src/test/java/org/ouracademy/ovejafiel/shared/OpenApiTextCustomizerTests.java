package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiTextCustomizerTests {

  @Test
  void javadocWrappedOverSeveralLinesBecomesOneLine() {
    assertThat(OpenApiTextCustomizer.plain("Removes the person from the node\n and what hangs."))
        .isEqualTo("Removes the person from the node and what hangs.");
  }

  @Test
  void codeIsWrittenInBackquotesInsteadOfHtml() {
    assertThat(OpenApiTextCustomizer.plain("Needs <code>\n members.write</code> on that node."))
        .isEqualTo("Needs `members.write` on that node.");
  }

  @Test
  void paragraphMarksAreDropped() {
    assertThat(OpenApiTextCustomizer.plain("First.\n\n <p>Second.")).isEqualTo("First. Second.");
  }

  @Test
  void aLinkToAClassLeavesItsName() {
    assertThat(OpenApiTextCustomizer.plain("See {@link GetPublicPageResult GetPublicPageResult}."))
        .isEqualTo("See `GetPublicPageResult`.");
    assertThat(OpenApiTextCustomizer.plain("Same as {@link #growth}."))
        .isEqualTo("Same as `growth`.");
  }

  @Test
  void aMissingTextStaysMissing() {
    assertThat(OpenApiTextCustomizer.plain(null)).isNull();
  }
}
