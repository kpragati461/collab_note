(function () {
    const storedTheme = localStorage.getItem('collabnote-theme');
    const preferredTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    document.documentElement.dataset.theme = storedTheme || preferredTheme;

    window.setTheme = function (theme) {
        document.documentElement.dataset.theme = theme;
        localStorage.setItem('collabnote-theme', theme);
        document.querySelectorAll('[data-theme-choice]').forEach((button) => {
            button.classList.toggle('active', button.dataset.themeChoice === theme);
            button.setAttribute('aria-pressed', button.dataset.themeChoice === theme);
        });
    };

    document.addEventListener('DOMContentLoaded', () => {
        const theme = document.documentElement.dataset.theme;
        document.querySelectorAll('[data-theme-choice]').forEach((button) => {
            button.classList.toggle('active', button.dataset.themeChoice === theme);
            button.setAttribute('aria-pressed', button.dataset.themeChoice === theme);
        });
    });
}());
