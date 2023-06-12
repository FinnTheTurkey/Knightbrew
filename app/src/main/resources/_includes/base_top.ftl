<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <link rel="stylesheet" href="/css/style.css">
    <link rel="stylesheet" href="https://unpkg.com/aos@next/dist/aos.css" />
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Source+Sans+Pro:ital,wght@0,300;0,400;1,300;1,400&display=swap" rel="stylesheet">
    <link rel="apple-touch-icon" sizes="180x180" href="/icons/apple-touch-icon.png">
    <link rel="icon" type="image/png" sizes="32x32" href="/icons/favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="/icons/favicon-16x16.png">
    <link rel="manifest" href="/icons/manifest.webmanifest">
    <link rel="mask-icon" href="/icons/safari-pinned-tab.svg" color="#5bbad5">
    <link rel="shortcut icon" href="/icons/favicon.ico">
    <meta name="msapplication-TileColor" content="#603cba">
    <meta name="msapplication-config" content="/icons/browserconfig.xml">
    <meta name="theme-color" content="#0b0b0b">

    <!-- <!-1- I'm sorry, I was forced ;( -1-> -->
    <!-- <script async src="https://www.googletagmanager.com/gtag/js?id=G-Z1KRCWE0R3"></script> -->
    <!-- <script> -->
    <!--     window.dataLayer = window.dataLayer || []; -->

    <!--     function gtag() { -->
    <!--         dataLayer.push(arguments); -->
    <!--     } -->
    <!--     gtag('js', new Date()); -->

    <!--     gtag('config', 'G-Z1KRCWE0R3'); -->
    <!-- </script> -->
</head>

<body>
    <div class="page-container">

        <div class="content-wrap">
            <!-- <nav class="navbar"> -->
            <div class="navbar-top">
                <div class="mobile-menu" style="user-select: none;">
                    <span class="menu-icon open" onclick="openMenu()">&#x2261;</span>
                    <span class="menu-icon close" onclick="closeMenu()" hidden>&#x2715;</span>
                </div>
                <a class="title times" href="/">
                    <!-- <img src="{{ '/assets/logo.png' | url }}" alt="" width="30" height="30"
            class="" style="filter: invert(100%); image-rendering: crisp-edges;"> -->
                    <!-- <svg viewBox="0 0 295 58">
              <text x="0" y="50">Knightwatch</text>
            </svg> -->
                    Knightwatch
                </a>
                <p class="subtitle">Nepean High School's<br>Online Newspaper</p>
            </div>
            <div class="navbar-bottom" hidden>
                <ul class="topic-list">
                    <#list sections as sec>
			  <#if id != "all">
			    <li class="list-style">
			      <a class="nav-link <#if sec.slug == "section"> active </#if> "< href="/section/${sec.slug}">${sec.deslug}</a>
			    </li>
			  </#if>
			</#list>
                </ul>
            </div>
            <div class="timer" id="timer">
                Submission deadline for this month is...
            </div>
            <!-- </nav> -->

            <!-- Navbar -->
            <!-- <div style="display:flex">
      <img src="{{ '/assets/logo.png' | url }}" class="content-image" style="background: purple; margin-right: 30px;" />
      <div>
        <a href="{{ '/' | url }}">
          <h1>Home</h1>
        </a>
        {% for id, collection in collections | dictsort %}
          {% if id != "all" %}
            <a href="{{ ['/section/', id] | join | url }}">
              <h1>{{ id | deslug | title }}</h1>
            </a>
          {% endif %}
        {% endfor %}
      </div>
    </div> -->

            <!-- Seperation -->
            <!-- <hr> -->

            <!-- Main content -->
            <main>
