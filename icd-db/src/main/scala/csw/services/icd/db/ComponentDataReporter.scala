package csw.services.icd.db

import icd.web.shared.IcdModels.TelemetryModel

import collection.immutable.Set
import scala.collection.mutable
import scala.util.Try

object ComponentDataReporter {

    case class EventInfo(component: String, name: String, rate: Double, size: Int)

    def printAllUsedUnits(db: IcdDb): Unit = {
      var units = Set[String]()
      val components = db.query.getComponents
      components.foreach { componentModel =>
        println(s"--------- Component ${componentModel.component} --------")
        val publishModel = db.query.getPublishModel(componentModel)
        publishModel.foreach { model =>
          model.eventList.foreach { item =>
            println(s"----- Item ${item.name}")
            item.attributesList.foreach { att =>
              print(s"-- Attribute ${att.name}: type=${att.typeStr}")
              units = units + att.units
            }
          }
          model.telemetryList.foreach { item =>
            println(s"----- Item ${item.name}")
            item.attributesList.foreach { att =>
              print(s"-- Attribute ${att.name}: type=${att.typeStr}")
              units = units + att.units
            }
          }
          model.eventStreamList.foreach { item =>
            println(s"----- Item ${item.name}")
            item.attributesList.foreach { att =>
              println(s"-- Attribute ${att.name}: type=${att.typeStr}")
              units = units + att.units
            }
          }
        }
        val commandModel = db.query.getCommandModel(componentModel)
        commandModel.foreach( model =>
          model.receive.foreach { command =>
            println(s"----- Command ${command.name}")
            command.args.foreach { arg =>
              println(s"-- Argument ${arg.name}: type=${arg.typeStr}")
              units = units + arg.units
            }
          }
        )
      }
      println(units.toSeq.sorted.mkString("\n\n\n----- Units -----\n", "\n", "\n------------"))
    }

    def listData(db: IcdDb, subsystem: String): Unit = {
      listArchivedDataRates(db, subsystem)
    }

    def listEvents(db: IcdDb, subsystem: String): Unit = {
      listEventManifest(db, subsystem)
    }

    def listEventManifest(db:IcdDb, subsystem: String): Unit = {
      val publishInfo = db.query.getPublishInfo(subsystem)
      val subsystemItems = mutable.MutableList[EventInfo]()
      publishInfo.foreach { componentPublishInfo =>
        println(s" ----  ${componentPublishInfo.componentName} ----- ")
        val componentModel = db.query.getComponentModel(subsystem, componentPublishInfo.componentName)
        val componentItems = mutable.MutableList[EventInfo]()
        componentModel.foreach { cm =>
          db.query.getPublishModel(cm).foreach { cpm =>
            if (cpm.eventList.nonEmpty) {
              println("--- Event Data")
              componentItems ++= processEventList(cpm.eventList, componentPublishInfo.componentName)
            }
            if (cpm.telemetryList.nonEmpty) {
              println("--- Telemetry Data")
              componentItems ++= processEventList(cpm.telemetryList, componentPublishInfo.componentName)
            }
            if (cpm.eventStreamList.nonEmpty) {
              println("--- EventSteam Data")
              componentItems ++= processEventList(cpm.eventStreamList, componentPublishInfo.componentName)
            }
          }
        }
        println(s"COMPONENT SUMMARY: ${componentPublishInfo.componentName}")
        printEventSummary(componentItems)
        subsystemItems ++= componentItems
      }
      println("SUBSYSTEM SUMMARY")
      println("-----------------")
      printEventSummary(subsystemItems)
    }

    private def printEventSummary(items: mutable.MutableList[EventInfo]): Unit = {
      val eventMap = new mutable.HashMap[Double, List[EventInfo]]() {override def default(key: Double): List[EventInfo] = List[EventInfo]()}
      items.foreach {item =>
        val newList = eventMap(item.rate) :+ item
        eventMap += ((item.rate, newList))
      }
      eventMap.toSeq.sortBy(_._1).foreach(item => printEventInfos(item._2))
    }

  private def printEventInfos(items: List[EventInfo]): Unit = items.foreach { item =>
    println(s"Rate = ${item.rate} Hz, Name = ${item.component}.${item.name}, Size = ${item.size}")
  }

    private def processEventList(items: List[TelemetryModel], component: String): List[EventInfo] = items.map(displayEvent(_, component))

    private def displayEvent(item: TelemetryModel, component: String): EventInfo = {
      println(s"Item Name: ${item.name}, min rate = ${item.minRate}, max rate=${item.maxRate}, archiveRate=${item.archiveRate}, archive=${item.archive}")
      val rate = getMaxRate(item)
      val size = getItemSize(item)
      println(s"-- size: $size bytes, max rate: $rate Hz")
      EventInfo(component, item.name, rate, size)
    }
    def listArchivedDataRates(db: IcdDb, subsystem: String): Unit= {
      val publishInfo = db.query.getPublishInfo(subsystem)
      var componentTotalDataRate = 0.0
      publishInfo.foreach { componentPublishInfo =>
         println(s" ----  ${componentPublishInfo.componentName} ----- ")
        componentTotalDataRate = 0.0
        val componentModel = db.query.getComponentModel(subsystem, componentPublishInfo.componentName)
        componentModel.foreach { cm =>
          db.query.getPublishModel(cm).foreach { cpm =>
            if (cpm.eventList.nonEmpty) {
              println("--- Event Data")
              componentTotalDataRate += listTelemetryData(cpm.eventList)
            }
            if (cpm.telemetryList.nonEmpty) {
              println("--- Telemetry Data")
              componentTotalDataRate += listTelemetryData(cpm.telemetryList)
            }
            if (cpm.eventStreamList.nonEmpty) {
              println("--- EventSteam Data")
              componentTotalDataRate += listTelemetryData(cpm.eventStreamList)
            }
          }
        }
        println(s"==== Total archived data rate for ${componentPublishInfo.componentName}: $componentTotalDataRate MB/hour")
      }
    }

    def listTelemetryData(items: List[TelemetryModel]): Double = {
      var totalDataRate = 0.0
      items.foreach { item =>
        println(s"Item Name: ${item.name}, min rate = ${item.minRate}, max rate=${item.maxRate}, archiveRate=${item.archiveRate}, archive=${item.archive}")
        if (item.archive) {
          val rate = getRate(item)
          println(s"Item is archived at a rate of $rate Hz")
          val itemData = getItemSize(item)
          val dataRate = itemData * rate * 3600.0 / 1000000.0
          if (itemData > 0) {
            totalDataRate += dataRate
            println(s"Total size of event: $itemData bytes.  data rate: $dataRate MB/hour")
          }
        }
      }
      println(s"Total data rate for this component model: $totalDataRate")
      totalDataRate
    }

    private def getRate(item: TelemetryModel) = {
      val DEFAULT_RATE = 0.1 // TODO move somewhere
      if (item.archiveRate > 0.0) {
        item.archiveRate
      } else if (item.maxRate > 0.0) {
        item.maxRate
      } else if (item.minRate > 0.0) {
        item.minRate
      } else {
        DEFAULT_RATE
      }
    }

    private def getMaxRate(item: TelemetryModel) = {
      val DEFAULT_RATE = 0.01 // TODO move somewhere
      List(item.archiveRate, item.maxRate, item.minRate, DEFAULT_RATE).max
    }

    private def getItemSize(item: TelemetryModel) = {
      var itemData = 8 // 8-bytes for timestamp
      item.attributesList.foreach { att =>
        print(s"-- Attribute ${att.name}: type=${att.typeStr}")
        if (att.typeStr.startsWith("array of ")) {
          // if dimensions not specified, use maxItems.  If that is not specified, use 1
          val numItems = Try(att.maxItems.getOrElse("1").toInt).getOrElse(1)
          getSizeOfType(att.typeStr.drop(9)) match {
            case Some(s) =>
              val sz = s * numItems
              println(s", size=$sz byte(s)")
              itemData += sz
            case None => println(", cannot determine event size.  Skipping")
          }
        } else {
          getSizeOfType(att.typeStr) match {
            case Some(s) =>
              println(s", size=$s byte(s)")
              itemData += s
            case None => println(", cannot determine event size.  Skipping")
          }
        }
      }
      itemData
    }

    private def getSizeOfType(dtype: String): Option[Int] = dtype match {
      case s if s.startsWith("boolean") => Some(1)
      case s if s.startsWith("byte") => Some(1)
      case s if s.startsWith("short") => Some(2)
      case s if s.startsWith("enum") => Some(4)
      case s if s.startsWith("integer") => Some(4)
      case s if s.startsWith("float") => Some(4)
      case s if s.startsWith("long") => Some(8)
      case s if s.startsWith("double") => Some(8)
      case s if s.startsWith("string") => Some(80)
      case s if s.startsWith("array[") =>
        var s1 = s.drop(6)
        var numElements = 1
        var commaLoc = s1.indexOf(',')
        val endLoc = s1.indexOf(']')
        if (commaLoc < endLoc) {
          while (commaLoc != -1) {
            val s2 = s1.take(commaLoc)
            numElements *= s2.toInt
            s1 = s1.drop(commaLoc + 1)
            commaLoc = s1.indexOf(',')
          }
        }
        val newEndLoc = s1.indexOf(']')
        val s3 = s1.substring(0, newEndLoc)
        numElements *= s3.toInt
        s1 = s1.drop(newEndLoc + 5)
        getSizeOfType(s1).map(_ * numElements)
      case _ => None
    }

}
