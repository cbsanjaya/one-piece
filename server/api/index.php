<?php

include('./lib/download.php');

if (!isset($_SERVER["PATH_INFO"])) {
    die("Route Can't be Empty");
}

$pathInfo = $_SERVER["PATH_INFO"];

$trim = trim($pathInfo, '/');

$url = explode("/", $trim);

if (count($url) == 1) {
    $file = __DIR__ . '/comic/' . $url[0] . '/index.json';
    if (file_exists($file)) {
        readfile($file);
    } else {
        downloadComic($file, $url[0]);
    }
} elseif (count($url) == 2) {
    $file = __DIR__ . '/comic/' . $url[0] . '/' . $url[1] . '.json';
    if (file_exists($file)) {
        readfile($file);
    } else {
        downloadChapter($file, $url[0], $url[1]);
    }
} else {
    die("Route not Found");
}
