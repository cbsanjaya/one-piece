<?php

namespace Models;

class Comic
{
    public $name;

    public $chapter;

    public $contents = array();

    public function __construct($name, $chapter, $contents)
    {
        $this->name = $name;
        $this->chapter = $chapter;
        $this->contents = $contents;
    }
}
