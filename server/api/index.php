<?php

// auto load class
// spl_autoload_register(function ($class_name) {
//     include $class_name . '.php';
// });

// $komik = new Models\Comic("one_piece", "1", array("lkj", "123"));

// echo json_encode($komik);

include('./lib/download_chapter.php');

if (!isset($_SERVER["PATH_INFO"])) {
    die("Route Can't be Empty");
}

$pathInfo = $_SERVER["PATH_INFO"];

$trim = trim($pathInfo, '/');

$url = explode("/", $trim);

if (count($url) == 1) {
    $file = __DIR__ . '/comic/' . $url[0] . '/index.json';
    if (!file_exists($file)) {
        echo 'Comic ' . $url[0] . ' not Found';
        exit();
    }

    readfile($file);
} elseif (count($url) == 2) {
    $file = __DIR__ . '/comic/' . $url[0] . '/' . $url[1] . '.json';
    if (file_exists($file)) {
        readfile($file);
    } else {
        comicChapter($file, $url[0], $url[1]);
    }
} else {
    die("Route not Found");
}
