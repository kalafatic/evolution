class NavControls {
    constructor() {
        this.navContainer = document.getElementById('nav-controls');
    }

    show() {
        if (this.navContainer) this.navContainer.style.display = 'flex';
    }

    hide() {
        if (this.navContainer) this.navContainer.style.display = 'none';
    }
}

export default NavControls;
