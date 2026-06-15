# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/js/

## Domain: general

## Components
* `navigator.js`: (function() { window.onerror = function(message, source, lineno, colno, error) { const errorMsg = "JS Error: " + message + " at " + source + ":" + lineno + ":" + colno; if (window.logFunction) window.logFunction(errorMsg); return false; }; const svg = document.getElementById("architecture-svg"); const container = document.getElementById("architecture-container"); let graphData = { nodes: [], links: [] }; let currentLayout = 'force'; let zoomScale = 1; let zoomX = 0; let zoomY = 0;
