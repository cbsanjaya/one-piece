<?php
    // include lib
    include('simple_html_dom.php');

    // echo $_GET["q"]; // chapter.php?q=102 -> 102
    // echo $_SERVER["QUERY_STRING"]; // chapter.php?q=102 -> q-102
    
    $chapter = $_GET["q"];
    $filename = getcwd() . "/chapter/" . $chapter . ".html";
    if ( file_exists($filename) ) {
        readfile($filename);
        exit;
    }

    const BASE_URL = "http://www.mangacanblog.com/" .
    "baca-komik-one_piece-%s-%d-bahasa-indonesia-one_piece-%s-terbaru.html";

    const IMAGE_TEMPLATE = '<img src="%s" id="responsive-image">';

    const INDEX_TEMPLATE = '<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8">' .
        '<meta name="viewport" content="width=device-width, initial-scale=1">' .
        '<style>#responsive-image{width:100%%;height:auto;margin:auto}</style>' .
        '</head><body>%s</body></html>';
    
    $url = sprintf(BASE_URL, $chapter, $chapter + 1, $chapter);
    
    $html = file_get_html($url);
    foreach($html->find('div#manga img') as $e) {
        $imageUrl = $e->src;
        $imageSource .= sprintf(IMAGE_TEMPLATE, $imageUrl);
    }
    if (! isset($imageSource) ) {
        echo "Komik One Piece Chapter " . $chapter . " Tidak ditemukan";
        exit;
    }
    

    $index = sprintf(INDEX_TEMPLATE, $imageSource);
    echo $index;

    //save the file...
    $fh = fopen($filename, "w");
    fwrite($fh, $index);
    fclose($fh);
?>