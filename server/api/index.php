<?php

spl_autoload_register(function ($class_name) {
    include $class_name . '.php';
});

$komik = new Models\Comic("one_piece", "1", array("lkj", "123"));

echo json_encode($komik);
