<?php
    // include lib
    include('simple_html_dom.php');
    
    $cnfFile = "grapimage.cnf";
    $chapter = file_get_contents($cnfFile);
    $pathName = getcwd() . "/images/" . $chapter;
    if ( file_exists($pathName) ) {
        exit;
    }

    const BASE_DOMAIN = "http://www.mangacanblog.com/";
    const BASE_URL = BASE_DOMAIN . "baca-komik-one_piece-%s-%d-bahasa-indonesia-one_piece-%s-terbaru.html";

    $url = sprintf(BASE_URL, $chapter, $chapter + 1, $chapter);
    $html = file_get_html($url);
    
    $imagesSaved = false;
    foreach($html->find('div#manga img') as $k => $v) {
        $imageUrl = $v->src;
        $firstUrl = substr($imageUrl, 0, 4);
        if ($firstUrl != "http") {
            $imageUrl = BASE_DOMAIN . $imageUrl;
        }
        $k++;
        // get extension
        $ext = pathinfo($imageUrl, PATHINFO_EXTENSION);
        // destination file
        $destFile = $pathName . "/" . $k . "." .$ext;

        // create Directory if not exists
        if ( !file_exists($pathName) ) {
            mkdir($pathName);
        }
        // copy images to destination file 
        copy($imageUrl, $destFile);
        $imagesSaved = true;
    }

    if ($imagesSaved) {
        //increser chapter
        $chapter++;
        //save the file...
        $fh = fopen($cnfFile, "w");
        fwrite($fh, $chapter);
        fclose($fh);
    }
?>