<?php
// include lib
include('simple_html_dom.php');

// echo $_GET["q"]; // chapter.php?q=102 -> 102
// echo $_SERVER["QUERY_STRING"]; // chapter.php?q=102 -> q-102

$comic = $_GET["comic"];
$chapter = $_GET["chapter"];
$dir = getcwd() . "/comic/" . $comic;

if (!file_exists($dir)) {
    mkdir($dir, 0777, true);
}

$filename = $dir . "/" . $chapter . ".html";

if (file_exists($filename)) {
    readfile($filename);
    exit;
}
const BASE_DOMAIN = "http://www.mangacanblog.com/";
$BASE_URL = BASE_DOMAIN . "baca-komik-" . $comic . "-%s-%d-bahasa-indonesia-" . $comic . "-%s-terbaru.html";

const IMAGE_TEMPLATE = '<img src="%s" id="responsive-image">';

const INDEX_TEMPLATE = '<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8">' .
    '<meta name="viewport" content="width=device-width, initial-scale=1">' .
    '<style>#responsive-image{width:100%%;height:auto;margin:auto}</style>' .
    '</head><body>%s</body></html>';

$url = sprintf($BASE_URL, $chapter, $chapter + 1, $chapter);

$html = file_get_html($url);

$imageSource = "";

foreach ($html->find('div#manga img') as $e) {
    $imageUrl = $e->src;
    $firstUrl = substr($imageUrl, 0, 4);
    if ($firstUrl != "http") {
        $imageUrl = BASE_DOMAIN . $imageUrl;
    }
    $imageSource .= sprintf(IMAGE_TEMPLATE, $imageUrl);
}

if ($imageSource == "") {
    echo "Komik " . $comic . " Chapter " . $chapter . " Tidak ditemukan";
    exit;
}


$index = sprintf(INDEX_TEMPLATE, $imageSource);
echo $index;

//save the file...
$fh = fopen($filename, "w");
fwrite($fh, $index);
fclose($fh);
