document.addEventListener("DOMContentLoaded", function() {
    // Theme toggle logic
    const themeToggle = document.getElementById("themeToggle");
    const currentTheme = localStorage.getItem("theme") || "light";
    
    document.documentElement.setAttribute("data-theme", currentTheme);
    if (currentTheme === "dark") {
        if(themeToggle) themeToggle.checked = true;
    }
    
    if (themeToggle) {
        themeToggle.addEventListener("change", function() {
            if (this.checked) {
                document.documentElement.setAttribute("data-theme", "dark");
                localStorage.setItem("theme", "dark");
            } else {
                document.documentElement.setAttribute("data-theme", "light");
                localStorage.setItem("theme", "light");
            }
        });
    }

    // Active sidebar link selector logic
    const currentPath = window.location.pathname;
    const sidebarLinks = document.querySelectorAll(".sidebar .nav-link");
    sidebarLinks.forEach(link => {
        const href = link.getAttribute("href");
        if (href === currentPath || (href !== "/" && currentPath.startsWith(href))) {
            link.classList.add("active");
        } else {
            link.classList.remove("active");
        }
    });

    // SMTP Connection Test script
    const testSmtpBtn = document.getElementById("btnTestSmtp");
    if (testSmtpBtn) {
        testSmtpBtn.addEventListener("click", function() {
            const host = document.getElementById("host").value;
            const port = document.getElementById("port").value;
            const username = document.getElementById("username").value;
            const password = document.getElementById("password").value;
            const tls = document.getElementById("tls").checked;
            const ssl = document.getElementById("ssl").checked;
            const id = document.getElementById("id") ? document.getElementById("id").value : "";

            const statusText = document.getElementById("smtpTestStatus");
            statusText.className = "mt-2 text-info";
            statusText.innerText = "Testing connection, please wait...";
            testSmtpBtn.disabled = true;

            fetch("/smtp/test", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ host, port, username, password, tls, ssl, id })
            })
            .then(res => res.json())
            .then(data => {
                testSmtpBtn.disabled = false;
                if (data.success) {
                    statusText.className = "mt-2 text-success fw-bold";
                    statusText.innerText = "Success! " + data.message;
                } else {
                    statusText.className = "mt-2 text-danger fw-bold";
                    statusText.innerText = "Failed: " + data.message;
                }
            })
            .catch(err => {
                testSmtpBtn.disabled = false;
                statusText.className = "mt-2 text-danger fw-bold";
                statusText.innerText = "Error: " + err;
            });
        });
    }

    // Checkbox select all for companies bulk actions
    const selectAllCheckbox = document.getElementById("selectAll");
    if (selectAllCheckbox) {
        const rowCheckboxes = document.querySelectorAll(".select-row");
        const bulkDeleteBtn = document.getElementById("btnBulkDelete");

        selectAllCheckbox.addEventListener("change", function() {
            rowCheckboxes.forEach(cb => {
                cb.checked = selectAllCheckbox.checked;
            });
            toggleBulkButton();
        });

        rowCheckboxes.forEach(cb => {
            cb.addEventListener("change", function() {
                toggleBulkButton();
            });
        });

        function toggleBulkButton() {
            const checkedCount = document.querySelectorAll(".select-row:checked").length;
            if (bulkDeleteBtn) {
                bulkDeleteBtn.disabled = checkedCount === 0;
            }
        }
    }
});
