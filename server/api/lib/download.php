<?php

function downloadComic($file, $comic)
{
    
    $dir = __DIR__ . '/../comic/' . $comic;
 
    if (!file_exists($dir)) {
        mkdir($dir, 0777, true);
    }

    // include lib
    include('./lib/simple_html_dom.php');

    //Link to download file...
    $rootUrl = "http://www.mangacanblog.com/";
    $url = $rootUrl . "baca-komik-" . $comic . "-bahasa-indonesia-online-terbaru.html";

    //Code to get the file...
    //$html = file_get_contents($url);
    
    // Retrieve the DOM from a given URL
    $html = file_get_html($url);

    $obj = new \stdClass;
    $obj->comic = $comic;

    $dataMentah = array();
    // Find the DIV tag with an id of "myId"
    foreach ($html->find('a.chaptersrec') as $e) {
        $title = $e->innertext;
        $titleImage = strpos($title, '<img');
        
        if (strpos($title, '<img')) {
            $title = substr($title, 0, $titleImage - 1);
        }

        $spliter =  strpos($title, ':');
        $spaceBeforeChapter = strrpos(substr($title, 0, $spliter - 2), ' ');
        $chapter = substr($title, $spaceBeforeChapter + 1, $spliter - $spaceBeforeChapter - 2);
        $dataMentah[] = array("chapter" => $chapter, "title" => $title);
    }

    $obj->contents = $dataMentah;
    $allData = json_encode($obj);
    
    if (count($dataMentah) == 0) {
        echo "Komik " . $comic . " Tidak ditemukan";
        exit();
    }
    
    $index = $allData;
    echo $index;
    
    //save the file...
    $fh = fopen($file, "w");
    fwrite($fh, $index);
    fclose($fh);
}

function downloadChapter($file, $comic, $chapter)
{
    // include lib
    include('simple_html_dom.php');
    
    $baseDomain = "http://www.mangacanblog.com/";
    $baseUrl = $baseDomain . "baca-komik-" . $comic . "-%s-%d-bahasa-indonesia-" . $comic . "-%s-terbaru.html";
    $url = sprintf($baseUrl, $chapter, $chapter + 1, $chapter);

    $html = file_get_html($url);
    
    $obj = new \stdClass;
    $obj->comic = $comic;
    $obj->chapter = $chapter;

    $imageSource = array();
    foreach ($html->find('div#manga img') as $e) {
        $imageUrl = $e->src;
        $firstUrl = substr($imageUrl, 0, 4);
        if ($firstUrl != "http") {
            $imageUrl = $baseDomain . $imageUrl;
        }

        $imageSource[] = $imageUrl;
    }

    $obj->images = $imageSource;
    $allData = json_encode($obj, JSON_UNESCAPED_SLASHES);

    if (count($imageSource) == 0) {
        echo "Komik " . $comic . " Chapter " . $chapter . " Tidak ditemukan";
        exit();
    }
    
    $index = $allData;
    echo $index;
    
    //save the file...
    $fh = fopen($file, "w");
    fwrite($fh, $index);
    fclose($fh);
}
