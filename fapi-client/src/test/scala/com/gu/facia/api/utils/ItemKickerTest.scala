package com.gu.facia.api.utils

import com.gu.contentapi.client.model.Content
import com.gu.facia.api.models.CollectionConfig
import com.gu.facia.client.models.{CollectionConfigJson, TrailMetaData}
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, OptionValues, ShouldMatchers, FreeSpec}
import org.mockito.Mockito._


class ItemKickerTest extends FreeSpec with ShouldMatchers with MockitoSugar with OptionValues with OneInstancePerTest {
  "fromContentAndTrail" - {
    val trailMetadata = Mockito.spy(TrailMetaData.empty)
    val content = mock[Content]
    val collectionConfig = Mockito.spy(CollectionConfig.fromCollectionJson(CollectionConfigJson.withDefaults()))

    "should prefer item level custom kicker to collection level section kicker" in {
      when(trailMetadata.customKicker).thenReturn(Some("custom kicker"))
      when(trailMetadata.showKickerCustom).thenReturn(Some(true))
      when(collectionConfig.showSections).thenReturn(true)

      ItemKicker.fromContentAndTrail(content, trailMetadata, Some(collectionConfig)).value shouldBe a [FreeHtmlKicker]
    }

    "should prefer item level section kicker to collection level tag kicker" in {
      when(collectionConfig.showTags).thenReturn(true)
      when(trailMetadata.showKickerSection).thenReturn(Some(true))
      when(content.sectionId).thenReturn(Some("section"))
      when(content.sectionName).thenReturn(Some("Section"))

      ItemKicker.fromContentAndTrail(content, trailMetadata, Some(collectionConfig)).value shouldBe a [SectionKicker]
    }
  }

  "kickerContents" - {
    "should return the contents of podcast kickers" in {
      ItemKicker.kickerContents(PodcastKicker(Some(Series("Name Goes Here", "")))) shouldBe Some("Name Goes Here")
    }

    "should return the contents of tag kickers" in {
      ItemKicker.kickerContents(TagKicker("Aberdeen-Grampian", "", "aberdeen-grampian/aberdeen-grampian")) shouldBe Some("Aberdeen-Grampian")
    }

    "should return the contents of free HTML kickers (without links)" in {
      ItemKicker.kickerContents(FreeHtmlKicker("<b>Something</b>")) shouldBe Some("<b>Something</b>")
    }

    "should return the contents of free HTML kickers (with links)" in {
      ItemKicker.kickerContents(FreeHtmlKickerWithLink("<b>Something</b>", "http://www.theguardian.com/football")) shouldBe Some("<b>Something</b>")
    }
  }

  "kickerText" - {
    "should return a textual description for Breaking News kickers" in {
      ItemKicker.kickerText(BreakingNewsKicker) shouldBe Some("Breaking")
    }

    "should return a textual description for Analysis kickers" in {
      ItemKicker.kickerText(AnalysisKicker) shouldBe Some("Analysis")
    }

    "should return a textual description for Review kickers" in {
      ItemKicker.kickerText(ReviewKicker) shouldBe Some("Review")
    }

    "should return a textual description for Cartoon kickers" in {
      ItemKicker.kickerText(CartoonKicker) shouldBe Some("Cartoon")
    }

    "should return a textual description for tag kickers" in {
      ItemKicker.kickerText(TagKicker("Aberdeen-Grampian", "", "aberdeen-grampian/aberdeen-grampian")) shouldBe Some("Aberdeen-Grampian")
    }

    "should return a textual description for section kickers" in {
      ItemKicker.kickerText(SectionKicker("Football", "")) shouldBe Some("Football")
    }

    "should return nothing for free HTML kickers containing HTML" in {
      ItemKicker.kickerText(FreeHtmlKicker("<b>Something</b>")) shouldBe None
      ItemKicker.kickerText(FreeHtmlKickerWithLink("<b>Something</b>", "http://www.theguardian.com/football")) shouldBe None
      ItemKicker.kickerText(FreeHtmlKickerWithLink("<a href=\"foo\">Something</b>", "http://www.theguardian.com/football")) shouldBe None
    }

    "should return the text of free HTML kickers not actually containing HTML" in {
      ItemKicker.kickerText(FreeHtmlKicker("Something")) shouldBe Some("Something")
      ItemKicker.kickerText(FreeHtmlKickerWithLink("Something", "http://www.theguardian.com/football")) shouldBe Some("Something")
    }
  }
}
