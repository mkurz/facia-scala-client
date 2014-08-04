package com.gu.facia.client.models

import org.joda.time.DateTime
import play.api.libs.json.Json

sealed trait MetaDataCommonFields {
  val headline: Option[String]
  val href: Option[String]
  val snapType: Option[String]
  val snapCss: Option[String]
  val snapUri: Option[String]
  val trailText: Option[String]
  val group: Option[String]
  val imageAdjust: Option[String]
  val imageSrc: Option[String]
  val imageSrcWidth: Option[String]
  val imageSrcHeight: Option[String]
  val isBreaking: Option[Boolean]
}

object SupportingItemMetaData {
  implicit val jsonReads = Json.reads[SupportingItemMetaData]

  val empty = SupportingItemMetaData(
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )
}

case class SupportingItemMetaData(
  headline: Option[String],
  href: Option[String],
  snapType: Option[String],
  snapCss: Option[String],
  snapUri: Option[String],
  trailText: Option[String],
  group: Option[String],
  imageAdjust: Option[String],
  imageSrc: Option[String],
  imageSrcWidth: Option[String],
  imageSrcHeight: Option[String],
  isBreaking: Option[Boolean]
) extends MetaDataCommonFields

object SupportingItem {
  implicit val jsonReads = Json.reads[SupportingItem]
}

case class SupportingItem(
  id: String,
  meta: Option[SupportingItemMetaData]
)

object TrailMetaData {
  implicit val jsonReads = Json.reads[TrailMetaData]

  val empty = TrailMetaData(
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )
}

case class TrailMetaData(
  headline: Option[String],
  href: Option[String],
  snapType: Option[String],
  snapCss: Option[String],
  snapUri: Option[String],
  trailText: Option[String],
  group: Option[String],
  imageAdjust: Option[String],
  imageSrc: Option[String],
  imageSrcWidth: Option[String],
  imageSrcHeight: Option[String],
  isBreaking: Option[Boolean],
  supporting: Option[List[SupportingItem]]
) extends MetaDataCommonFields

object Trail {
  implicit val jsonReads = Json.reads[Trail]
}

case class Trail(
  id: String,
  frontPublicationDate: Long,
  meta: TrailMetaData
)

object Collection {
  implicit val jsonReads = Json.reads[Collection]
}

case class Collection(
  name: Option[String],
  live: List[Trail],
  draft: Option[List[Trail]],
  lastUpdated: DateTime,
  updatedBy: String,
  updatedEmail: String,
  displayName: Option[String],
  href: Option[String]
)
