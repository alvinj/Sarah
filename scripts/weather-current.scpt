-- reads 'current conditions'
set weather to "curl " & quote & "http://weather.yahooapis.com/forecastrss?p=80020&u=f" & quote
set postWeather to "grep -E '(Current Conditions:|F<BR)'"
set forecast to do shell script weather & " | " & postWeather
(characters 1 through -7 of paragraph 2 of forecast) as string
say forecast
