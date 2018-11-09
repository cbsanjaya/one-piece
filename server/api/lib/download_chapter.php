<?php

function comicChapter($file, $comic, $chapter)
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
