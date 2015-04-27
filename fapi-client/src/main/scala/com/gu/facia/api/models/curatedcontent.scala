package com.gu.facia.api.models

import com.gu.contentapi.client.model.{Tag, Content}
import com.gu.facia.api.utils._
import com.gu.facia.client.models.{SupportingItem, MetaDataCommonFields, Trail, TrailMetaData}

case class FaciaImage(
  imageType: ImageType,
  imageSrc: String,
  imageSrcWidth: Option[String],
  imageSrcHeight: Option[String]
)

sealed trait ImageType
case object Cutout extends ImageType { override def toString = "cutout" }
case object Replace extends ImageType { override def toString = "replace" }
case object ImageDefault extends ImageType { override def toString = "default" }

object FaciaImage {

  def getFaciaImage(maybeContent: Option[Content], trailMeta: MetaDataCommonFields, resolvedMetadata: ResolvedMetaData = ResolvedMetaData.Default): Option[FaciaImage] = {
    if (resolvedMetadata.imageHide) None
    else { maybeContent flatMap { content =>
        if (resolvedMetadata.imageCutoutReplace) imageCutout(trailMeta) orElse fromContentTags(content, trailMeta)
        else None
      } orElse imageReplace(trailMeta)
    }
  }

  def fromContentTags(content: Content, trailMeta: MetaDataCommonFields): Option[FaciaImage] = {
    val contributorTags = content.tags.filter(_.`type` == "contributor")
    if (contributorTags.length == 1)
      for {
        tag <- contributorTags.find(_.bylineLargeImageUrl.isDefined)
        path <- tag.bylineLargeImageUrl
      } yield FaciaImage(Cutout, path, None, None)
    else None
  }

  def imageCutout(trailMeta: MetaDataCommonFields): Option[FaciaImage] = for {
    src <- trailMeta.imageCutoutSrc
    width <- trailMeta.imageCutoutSrcWidth
    height <- trailMeta.imageCutoutSrcHeight
  } yield FaciaImage(Cutout, src, Option(width), Option(height))

  def imageReplace(trailMeta: MetaDataCommonFields): Option[FaciaImage] = for {
    src <- trailMeta.imageSrc
    width <- trailMeta.imageSrcWidth
    height <- trailMeta.imageSrcHeight
    imageType = {if (trailMeta.imageReplace.exists(identity)) Replace else ImageDefault}
  } yield FaciaImage(imageType, src, Option(width), Option(height))

}

sealed trait FaciaContent

object Snap {
  val LatestType = "latest"
  val LinkType = "link"
  val DefaultType = LinkType

  def maybeFromTrail(trail: Trail): Option[Snap] = trail.safeMeta.snapType match {
    case Some("latest") =>
      Option(LatestSnap.fromTrailAndContent(trail, None))
    case Some(snapType) =>
      Option(LinkSnap(
      trail.id,
      snapType,
      trail.safeMeta.snapUri,
      trail.safeMeta.snapCss,
      trail.safeMeta.headline,
      trail.safeMeta.href,
      trail.safeMeta.trailText,
      trail.safeMeta.group.getOrElse("0"),
      FaciaImage.getFaciaImage(None, trail.safeMeta),
      trail.safeMeta.isBreaking.exists(identity),
      trail.safeMeta.isBoosted.exists(identity),
      trail.safeMeta.showMainVideo.exists(identity),
      trail.safeMeta.showKickerTag.exists(identity),
      trail.safeMeta.byline,
      trail.safeMeta.showByline.exists(identity),
      ItemKicker.fromTrailMetaData(trail.safeMeta),
      trail.safeMeta.showBoostedHeadline.exists(identity),
      trail.safeMeta.showQuotedHeadline.exists(identity)))
    case _ => None
  }

  def maybeFromSupportingItem(supportingItem: SupportingItem): Option[Snap] = supportingItem.safeMeta.snapType match {
    case Some("latest") =>
      Option(LatestSnap.fromSupportingItemAndContent(supportingItem, None))
    case Some(snapType) =>
      Option(LinkSnap(
      supportingItem.id,
      snapType,
      supportingItem.safeMeta.snapUri,
      supportingItem.safeMeta.snapCss,
      supportingItem.safeMeta.headline,
      supportingItem.safeMeta.href,
      supportingItem.safeMeta.trailText,
      supportingItem.safeMeta.group.getOrElse("0"),
      FaciaImage.getFaciaImage(None, supportingItem.safeMeta),
      supportingItem.safeMeta.isBreaking.exists(identity),
      supportingItem.safeMeta.isBoosted.exists(identity),
      supportingItem.safeMeta.showMainVideo.exists(identity),
      supportingItem.safeMeta.showKickerTag.exists(identity),
      supportingItem.safeMeta.byline,
      supportingItem.safeMeta.showByline.exists(identity),
      ItemKicker.fromTrailMetaData(supportingItem.safeMeta),
      supportingItem.safeMeta.showBoostedHeadline.exists(identity),
      supportingItem.safeMeta.showQuotedHeadline.exists(identity)
  ))
    case _ => None
  }
}

sealed trait Snap extends FaciaContent
case class LinkSnap(
  id: String,
  snapType: String,
  snapUri: Option[String],
  snapCss: Option[String],
  headline: Option[String],
  href: Option[String],
  trailText: Option[String],
  group: String,
  image: Option[FaciaImage],
  isBreaking: Boolean,
  isBoosted: Boolean,
  showMainVideo: Boolean,
  showKickerTag: Boolean,
  byline: Option[String],
  showByLine: Boolean,
  kicker: Option[ItemKicker],
  showBoostedHeadline: Boolean,
  showQuotedHeadline: Boolean) extends Snap

case class LatestSnap(
  id: String,
  cardStyle: CardStyle,
  snapUri: Option[String],
  snapCss: Option[String],
  latestContent: Option[Content],
  headline: Option[String],
  href: Option[String],
  trailText: Option[String],
  group: String,
  image: Option[FaciaImage],
  properties: ContentProperties,
  byline: Option[String],
  kicker: Option[ItemKicker]) extends Snap

object LatestSnap {
  def fromTrailAndContent(trail: Trail, maybeContent: Option[Content]): LatestSnap = {
    val cardStyle: CardStyle = maybeContent.map(CardStyle.apply(_, trail.safeMeta)).getOrElse(DefaultCardstyle)
    val resolvedMetaData: ResolvedMetaData =
      maybeContent.fold(ResolvedMetaData.fromTrailMetaData(trail.safeMeta))(ResolvedMetaData.fromContentAndTrailMetaData(_, trail.safeMeta, cardStyle))
    LatestSnap(
      trail.id,
      cardStyle,
      trail.safeMeta.snapUri,
      trail.safeMeta.snapCss,
      maybeContent,
      trail.safeMeta.headline,
      trail.safeMeta.href,
      trail.safeMeta.trailText,
      trail.safeMeta.group.getOrElse("0"),
      FaciaImage.getFaciaImage(maybeContent,  trail.safeMeta),
      ContentProperties.fromResolvedMetaData(resolvedMetaData),
      trail.safeMeta.byline,
      ItemKicker.fromMaybeContentTrailMetaAndResolvedMetaData(maybeContent, trail.safeMeta, resolvedMetaData)
    )
  }

  def fromSupportingItemAndContent(supportingItem: SupportingItem, maybeContent: Option[Content]): LatestSnap = {
    val cardStyle: CardStyle = maybeContent.map(CardStyle.apply(_, supportingItem.safeMeta)).getOrElse(DefaultCardstyle)
    val resolvedMetaData: ResolvedMetaData =
      maybeContent.fold(ResolvedMetaData.fromTrailMetaData(supportingItem.safeMeta))(ResolvedMetaData.fromContentAndTrailMetaData(_, supportingItem.safeMeta, cardStyle))
    LatestSnap(
      supportingItem.id,
      cardStyle,
      supportingItem.safeMeta.snapUri,
      supportingItem.safeMeta.snapCss,
      maybeContent,
      supportingItem.safeMeta.headline,
      supportingItem.safeMeta.href,
      supportingItem.safeMeta.trailText,
      supportingItem.safeMeta.group.getOrElse("0"),
      FaciaImage.getFaciaImage(maybeContent, supportingItem.safeMeta),
      ContentProperties.fromResolvedMetaData(resolvedMetaData),
      supportingItem.safeMeta.byline,
      ItemKicker.fromMaybeContentTrailMetaAndResolvedMetaData(maybeContent, supportingItem.safeMeta, resolvedMetaData)
    )
  }
}

case class CuratedContent(
  content: Content,
  supportingContent: List[FaciaContent],
  cardStyle: CardStyle,
  headline: String,
  href: Option[String],
  trailText: Option[String],
  group: String,
  image: Option[FaciaImage],
  properties: ContentProperties,
  byline: Option[String],
  kicker: Option[ItemKicker],
  embedType: Option[String],
  embedUri: Option[String],
  embedCss: Option[String]) extends FaciaContent

case class SupportingCuratedContent(
  content: Content,
  cardStyle: CardStyle,
  headline: String,
  href: Option[String],
  trailText: Option[String],
  group: String,
  image: Option[FaciaImage],
  properties: ContentProperties,
  byline: Option[String],
  kicker: Option[ItemKicker]) extends FaciaContent

object CuratedContent {

  def fromTrailAndContentWithSupporting(content: Content, trailMetaData: TrailMetaData,
                                        supportingContent: List[FaciaContent],
                                        collectionConfig: CollectionConfig) = {
    val contentFields: Map[String, String] = content.safeFields
    val cardStyle = CardStyle(content, trailMetaData)
    val resolvedMetaData = ResolvedMetaData.fromContentAndTrailMetaData(content, trailMetaData, cardStyle)

    CuratedContent(
      content,
      supportingContent,
      cardStyle,
      trailMetaData.headline.orElse(content.safeFields.get("headline")).getOrElse(content.webTitle),
      trailMetaData.href.orElse(contentFields.get("href")),
      trailMetaData.trailText.orElse(contentFields.get("trailText")),
      trailMetaData.group.getOrElse("0"),
      FaciaImage.getFaciaImage(Some(content), trailMetaData),
      ContentProperties.fromResolvedMetaData(resolvedMetaData),
      trailMetaData.byline.orElse(contentFields.get("byline")),
      ItemKicker.fromContentAndTrail(Option(content), trailMetaData, resolvedMetaData, Some(collectionConfig)),
      embedType = trailMetaData.snapType,
      embedUri = trailMetaData.snapUri,
      embedCss = trailMetaData.snapCss)}

  def fromTrailAndContent(content: Content, trailMetaData: MetaDataCommonFields, collectionConfig: CollectionConfig): CuratedContent = {
    val contentFields: Map[String, String] = content.safeFields
    val cardStyle = CardStyle(content, trailMetaData)
    val resolvedMetaData = ResolvedMetaData.fromContentAndTrailMetaData(content, trailMetaData, cardStyle)

    CuratedContent(
      content,
      supportingContent = Nil,
      cardStyle = cardStyle,
      trailMetaData.headline.orElse(content.safeFields.get("headline")).getOrElse(content.webTitle),
      trailMetaData.href.orElse(contentFields.get("href")),
      trailMetaData.trailText.orElse(contentFields.get("trailText")),
      trailMetaData.group.getOrElse("0"),
      FaciaImage.getFaciaImage(Some(content), trailMetaData),
      ContentProperties.fromResolvedMetaData(resolvedMetaData),
      trailMetaData.byline.orElse(contentFields.get("byline")),
      ItemKicker.fromContentAndTrail(Option(content), trailMetaData, resolvedMetaData, Some(collectionConfig)),
      embedType = trailMetaData.snapType,
      embedUri = trailMetaData.snapUri,
      embedCss = trailMetaData.snapCss)}
}

object SupportingCuratedContent {
  def fromTrailAndContent(content: Content, trailMetaData: MetaDataCommonFields, collectionConfig: CollectionConfig): SupportingCuratedContent = {
    val contentFields: Map[String, String] = content.safeFields
    val cardStyle = CardStyle(content, trailMetaData)
    val resolvedMetaData = ResolvedMetaData.fromContentAndTrailMetaData(content, trailMetaData, cardStyle)

    SupportingCuratedContent(
      content,
      cardStyle,
      trailMetaData.headline.orElse(content.safeFields.get("headline")).getOrElse(content.webTitle),
      trailMetaData.href.orElse(contentFields.get("href")),
      trailMetaData.trailText.orElse(contentFields.get("trailText")),
      trailMetaData.group.getOrElse("0"),
      FaciaImage.getFaciaImage(Some(content), trailMetaData),
      ContentProperties.fromResolvedMetaData(resolvedMetaData),
      trailMetaData.byline.orElse(contentFields.get("byline")),
      ItemKicker.fromContentAndTrail(Option(content), trailMetaData, resolvedMetaData, None)
    )
  }
}