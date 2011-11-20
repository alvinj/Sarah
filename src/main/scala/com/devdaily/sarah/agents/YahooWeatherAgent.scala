package com.devdaily.sarah.agents

import java.io._
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import scala.collection.mutable.StringBuilder
import scala.xml.XML

object YahooWeatherAgent {

  def getCurrentWeather():String = {
    val content = RestUtils.getRestContent("http://weather.yahooapis.com/forecastrss?p=80020&u=f")
    val xml = XML.loadString(content)
    val ytemp = (xml \\ "channel" \\ "item" \ "condition" \ "@temp").text
    val ytext = (xml \\ "channel" \\ "item" \ "condition" \ "@text").text
    format("The current temperature is %s degrees, and the sky is %s.", ytemp, ytext.toLowerCase())
  }

}


