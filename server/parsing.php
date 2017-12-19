<?php
    // include lib
    include('simple_html_dom.php');
    //Link to download file...
    $url = "http://www.mangacanblog.com/baca-komik-one_piece-bahasa-indonesia-online-terbaru.html";

    //Code to get the file...
    //$html = file_get_contents($url);
    
    // Retrieve the DOM from a given URL
    $html = file_get_html($url);
    // Find the DIV tag with an id of "myId"
    $dataMentah = array();
    $dataLast5 = array();
    foreach($html->find('a.chaptersrec') as $e) {
        $all = $e->innertext;
        $spliter =  strpos($all, ':');
        $chapter = substr($all, 10, $spliter - 11);
        $title = substr($all, $spliter + 2, strlen($all) -  $spliter);
        $titleImage = strpos($title, '<img'); 
        if (strpos($title, '<img')) {
            $title = substr($title, 0, $titleImage - 1);
        }
        $dataMentah[] = array("chapter" => $chapter, "title" => $title);
        if (count($dataLast5) < 5)
            $dataLast5[] = array("chapter" => $chapter, "title" => $title); 
    }

    $allData = json_encode($dataMentah);
    $last5Data = json_encode($dataLast5);

    function saveToFile($filename, $data) {
        //save the file...
        $fh = fopen($filename,"w");
        fwrite($fh,$data);
        fclose($fh);
    }

    saveToFile("all.json", $allData);
    saveToFile("last5.json", $last5Data);
?>