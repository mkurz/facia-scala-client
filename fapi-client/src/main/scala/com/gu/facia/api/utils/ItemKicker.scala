package com.gu.facia.api.utils

import com.gu.contentapi.client.model.{Content, Tag}
import com.gu.facia.api.models.CollectionConfig
import com.gu.facia.client.models.{MetaDataCommonFields, TrailMetaData}

object ItemKicker {
  def fromContentAndTrail(maybeContent: Option[Content], trailMeta: MetaDataCommonFields, config: Option[CollectionConfig]): Option[ItemKicker] = {
    lazy val maybeTag = maybeContent.flatMap(_.tags.headOption)
    lazy val tagKicker = maybeTag.map(TagKicker.fromTag)
    lazy val sectionKicker = for {
      content <- maybeContent
      name <- content.sectionName
      id <- content.sectionId
    } yield SectionKicker(name.capitalize, "/" + id)

    trailMeta.customKicker match {
      case Some(kicker)
        if trailMeta.snapType.exists(_.contains("latest")) &&
          trailMeta.showKickerCustom.exists(identity) &&
          trailMeta.snapUri.isDefined => Some(FreeHtmlKickerWithLink(kicker, s"/${trailMeta.snapUri.get}"))
      case Some(kicker) if trailMeta.showKickerCustom.exists(identity) => Some(FreeHtmlKicker(kicker))
      case _ => if (trailMeta.showKickerTag.exists(identity) && maybeTag.isDefined) {
        tagKicker
      } else if (trailMeta.showKickerSection.exists(identity)) {
        sectionKicker
      } else if (config.exists(_.showTags) && maybeTag.isDefined) {
        tagKicker
      } else if (config.exists(_.showSections)) {
        sectionKicker
      } else if (!config.exists(_.hideKickers) && maybeContent.isDefined) {
        tonalKicker(maybeContent.get, trailMeta)
      } else {
        None
      }
    }
  }

  def fromTrailMetaData(trailMeta: MetaDataCommonFields): Option[ItemKicker] = fromContentAndTrail(None, trailMeta, None)

  def fromContentAndTrail(content: Content, trailMeta: MetaDataCommonFields, config: Option[CollectionConfig]): Option[ItemKicker]
    = fromContentAndTrail(Option(content), trailMeta, config)

  private[utils] def tonalKicker(content: Content, trailMeta: MetaDataCommonFields): Option[ItemKicker] = {
    def tagsOfType(tagType: String): Seq[Tag] = content.tags.filter(_.`type` == tagType)
    val types: Seq[Tag] = tagsOfType("type")
    val tones: Seq[Tag] = tagsOfType("tone")

    lazy val isReview = tones.exists(t => Tags.reviewMappings.contains(t.id))
    lazy val isAnalysis = tones.exists(_.id == Tags.Analysis)
    lazy val isPodcast = types.exists(_.id == Tags.Podcast) || content.tags.exists(_.podcast.isDefined)
    lazy val isCartoon = types.exists(_.id == Tags.Cartoon)

    if (trailMeta.isBreaking.exists(identity)) {
      Some(BreakingNewsKicker)
    } else if (content.safeFields.get("liveBloggingNow").exists(_.toBoolean)) {
      Some(LiveKicker)
    } else if (isPodcast) {
      val series = content.tags.find(_.`type` == "series") map { seriesTag =>
        Series(seriesTag.webTitle, seriesTag.webUrl)
      }
      Some(PodcastKicker(series))
    } else if (isAnalysis) {
      Some(AnalysisKicker)
    } else if (isReview) {
      Some(ReviewKicker)
    } else if (isCartoon) {
      Some(CartoonKicker)
    } else {
      None
    }
  }

  val TagsThatDoNotAutoKicker = Set(
    "commentisfree/commentisfree"
  )

  def seriesOrBlogKicker(item: Content) =
    item.tags.find({ tag =>
      Set("series", "blog").contains(tag.`type`) && !TagsThatDoNotAutoKicker.contains(tag.id)
    }).map(TagKicker.fromTag)

  /** Used for de-duping bylines */
  def kickerContents(itemKicker: ItemKicker): Option[String] = itemKicker match {
    case PodcastKicker(Some(series)) => Some(series.name)
    case TagKicker(name, _, _) => Some(name)
    case SectionKicker(name, _) => Some(name)
    case FreeHtmlKicker(body) => Some(body)
    case FreeHtmlKickerWithLink(body, _) => Some(body)
    case _ => None
  }

  def stringIfPlainText(string: String) : Option[String] = {
    val StringWithHtml = "<\\w+.*>".r.unanchored
    string match {
      case StringWithHtml() => None
      case _ => Some(string)
    }
  }

  /** Return a plain-text representation of a kicker */
  def kickerText(itemKicker: ItemKicker): Option[String] = itemKicker match {
    case BreakingNewsKicker => Some("Breaking")
    case AnalysisKicker => Some("Analysis")
    case ReviewKicker => Some("Review")
    case CartoonKicker => Some("Cartoon")
    case FreeHtmlKicker(text) => stringIfPlainText(text)
    case FreeHtmlKickerWithLink(text, _) => stringIfPlainText(text)
    case _ => kickerContents(itemKicker)
  }
}

case class Series(name: String, url: String)

sealed trait ItemKicker

case object BreakingNewsKicker extends ItemKicker
case object LiveKicker extends ItemKicker
case object AnalysisKicker extends ItemKicker
case object ReviewKicker extends ItemKicker
case object CartoonKicker extends ItemKicker
case class PodcastKicker(series: Option[Series]) extends ItemKicker

object TagKicker {
  def fromTag(tag: Tag) = TagKicker(tag.webTitle, tag.webUrl, tag.id)
}

case class TagKicker(name: String, url: String, id: String) extends ItemKicker

case class SectionKicker(name: String, url: String) extends ItemKicker
case class FreeHtmlKicker(body: String) extends ItemKicker
case class FreeHtmlKickerWithLink(body: String, url: String) extends ItemKicker


object Tags {
  val Analysis = "tone/analysis"
  val Crossword = "type/crossword"
  val Podcast = "type/podcast"
  val Editorial = "tone/editorials"
  val Cartoon = "type/cartoon"
  val Letters = "tone/letters"

  object VisualTone {
    val Live = "live"
    val Comment = "comment"
    val Feature = "feature"
    val News = "news"
  }

  val liveMappings = Seq(
    "tone/minutebyminute"
  )

  val commentMappings = Seq (
    "tone/comment"
  )

  val mediaTypes = Seq(
    "type/video",
    "type/audio",
    "type/gallery",
    "type/picture"
  )

  val featureMappings = Seq(
    "tone/features",
    "tone/recipes",
    "tone/interview",
    "tone/performances",
    "tone/extract",
    "tone/reviews",
    "tone/albumreview",
    "tone/livereview",
    "tone/childrens-user-reviews"
  )

  val reviewMappings = Seq(
    "tone/reviews"
  )
}