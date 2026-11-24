/*
 * EVO Forge shared interactive zoom/pan component.
 *
 * One reusable implementation for every Forge visualization (embedded SWT Browser and
 * external browser). Provides pointer-centered wheel zoom, click-drag pan, zoom in/out,
 * fit, reset and a zoom-percentage indicator, applied to any container marked with
 * class "viz-pane" or attribute data-viz-pane.
 *
 * SVG diagrams are transformed via CSS transform on the <svg> element; canvas diagrams
 * use the same pane-level coordinate transform without touching the canvas pixel buffer,
 * so hit-testing and re-draws remain unaffected (the canvas draws in its own coordinate
 * space and the pane transform is purely presentational).
 */
(function (global) {
    'use strict';
    var MIN = 0.2, MAX = 8.0;

    function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }

    function interact(pane) {
        if (!pane || pane.__evoVizInit) return;
        pane.__evoVizInit = true;

        var target = pane.querySelector('svg, canvas');
        if (!target) target = pane;

        pane.classList.add('viz-pane');
        var st = { s: 1, tx: 0, ty: 0 };
        target.style.transformOrigin = '0 0';
        target.style.willChange = 'transform';

        function apply() {
            target.style.transform = 'translate(' + st.tx + 'px,' + st.ty + 'px) scale(' + st.s + ')';
            var l = pane.querySelector('.viz-zoom-level');
            if (l) l.textContent = Math.round(st.s * 100) + '%';
        }

        var bar = document.createElement('div');
        bar.className = 'viz-toolbar';
        bar.innerHTML =
            '<button data-a="in" title="Zoom in">+</button>' +
            '<button data-a="out" title="Zoom out">\u2212</button>' +
            '<button data-a="fit" title="Fit to view">Fit</button>' +
            '<button data-a="reset" title="Reset">Reset</button>' +
            '<span class="viz-zoom-level">100%</span>';
        pane.insertBefore(bar, pane.firstChild);

        bar.addEventListener('click', function (e) {
            var b = e.target && e.target.closest ? e.target.closest('button[data-a]') : null;
            if (!b) return;
            var a = b.getAttribute('data-a');
            if (a === 'in') zoom(1.25);
            else if (a === 'out') zoom(0.8);
            else if (a === 'fit' || a === 'reset') { st.s = 1; st.tx = 0; st.ty = 0; apply(); }
        });

        function zoom(factor, cx, cy) {
            var r = pane.getBoundingClientRect();
            if (cx == null) cx = r.width / 2;
            if (cy == null) cy = r.height / 2;
            var ns = clamp(st.s * factor, MIN, MAX);
            var k = ns / st.s;
            st.tx = cx - (cx - st.tx) * k;
            st.ty = cy - (cy - st.ty) * k;
            st.s = ns;
            apply();
        }

        if (typeof window.addEventListener === 'function') {
            pane.addEventListener('wheel', function (e) {
                e.preventDefault();
                var r = pane.getBoundingClientRect();
                zoom(e.deltaY < 0 ? 1.15 : 0.87, e.clientX - r.left, e.clientY - r.top);
            }, { passive: false });
        }

        var dragging = false, sx = 0, sy = 0, otx = 0, oty = 0;
        function down(x, y, id) {
            dragging = true; sx = x; sy = y; otx = st.tx; oty = st.ty;
            pane.style.cursor = 'grabbing';
            if (id != null && pane.setPointerCapture) { try { pane.setPointerCapture(id); } catch (e) {} }
        }
        function move(x, y) { if (!dragging) return; st.tx = otx + (x - sx); st.ty = oty + (y - sy); apply(); }
        function up() { dragging = false; pane.style.cursor = 'grab'; }

        var hasPointer = ('PointerEvent' in window);
        if (hasPointer) {
            pane.addEventListener('pointerdown', function (e) { down(e.clientX, e.clientY, e.pointerId); });
            pane.addEventListener('pointermove', function (e) { move(e.clientX, e.clientY); });
            pane.addEventListener('pointerup', up);
            pane.addEventListener('pointercancel', up);
        } else {
            pane.addEventListener('mousedown', function (e) { down(e.clientX, e.clientY, null); });
            pane.addEventListener('mousemove', function (e) { move(e.clientX, e.clientY); });
            pane.addEventListener('mouseup', up);
            pane.addEventListener('mouseleave', up);
        }

        pane.style.cursor = 'grab';
        pane.style.overflow = 'hidden';
        pane.style.userSelect = 'none';
        pane.style.touchAction = 'none';
        apply();
    }

    function initAll(root) {
        var scope = root || document;
        scope.querySelectorAll('.viz-pane, [data-viz-pane]').forEach(interact);
    }

    global.EvoViz = global.EvoViz || {};
    global.EvoViz.interact = interact;
    global.EvoViz.initAll = initAll;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { initAll(); });
    } else {
        initAll();
    }

    // Auto-initialize panes that are added after a viz module re-renders (the Forge
    // training monitor replaces its innerHTML on telemetry updates). The interaction
    // state (zoom/pan) is per-element and is naturally lost on a full re-render, which
    // is the correct behaviour for a completely rebuilt chart.
    if (typeof MutationObserver === 'function') {
        var observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (m) {
                m.addedNodes.forEach(function (node) {
                    if (node.nodeType !== 1) return;
                    if (node.classList && (node.classList.contains('viz-pane') || node.hasAttribute('data-viz-pane'))) {
                        interact(node);
                    }
                    if (node.querySelectorAll) {
                        node.querySelectorAll('.viz-pane, [data-viz-pane]').forEach(interact);
                    }
                });
            });
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }
})(window);
