# YaPhotosNoLib

Sample Android app to demonstrate usage of Yandex.Fotki API.

Displays three public photo albums: "Recent photos", "Popular photos" and "Photos of the day".

This version does not use any libraries, so app size is very small (< 100 Kb).

![Album view](app/src/main/screen_album.png)  ![Photo view](app/src/main/screen_photo.png)  ![Fullscreen view](app/src/main/screen_full.png)

## Features

* Album and detail view
* Thumbnails caching and preloading
* Endless scrolling
* Fullscreen mode
* Progress and error indicators
* Image quality, thumbnail size and column number auto-adjusted to screen size
* Workaround to calculate next page for "Photos of the day" album

## Technologies used

* AsyncTask
* AsyncTaskLoader
* Date, Calendar, SimpleDateFormat
* Fragment, FragmentManager
* GridView, ArrayAdapter
* HandlerThread, Handler, Message
* HttpURLConnection
* JSONObject, JSONArray
* LruCache
* SharedPreferences
* StrictMode

## Installation

This is an Android Studio project.

## License

Project is distributed under MIT license.

The use of Yandex.Fotki service and its API is regulated by [API User Agreement](https://yandex.ru/legal/fotki_api/), [Yandex.Fotki Service Terms Of Use](https://yandex.ru/legal/fotki_termsofuse/) and general [User Agreement for Yandex Services](https://yandex.com/legal/rules/).