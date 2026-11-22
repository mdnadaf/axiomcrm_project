// AcxiomCRM Client-Side Form Validation and Helper Utilities

document.addEventListener("DOMContentLoaded", function () {
    // Enable Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Client-side form validation using HTML5 + custom rules
    var forms = document.querySelectorAll(".needs-validation");
    Array.prototype.slice.call(forms).forEach(function (form) {
        form.addEventListener("submit", function (event) {
            var isValid = true;

            // 1. Phone validation: 10-digit Indian mobile pattern: ^[6-9]\d{9}$
            var phoneInputs = form.querySelectorAll("input[type='tel'], input[name='phone']");
            phoneInputs.forEach(function (input) {
                if (input.value && input.value.trim() !== "") {
                    var phoneRegex = /^[6-9]\d{9}$/;
                    if (!phoneRegex.test(input.value.trim())) {
                        input.setCustomValidity("Enter a valid phone number.");
                        isValid = false;
                    } else {
                        input.setCustomValidity("");
                    }
                }
            });

            // 2. Email validation
            var emailInputs = form.querySelectorAll("input[type='email'], input[name='email']");
            emailInputs.forEach(function (input) {
                if (input.value && input.value.trim() !== "") {
                    var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                    if (!emailRegex.test(input.value.trim())) {
                        input.setCustomValidity("Enter a valid email address.");
                        isValid = false;
                    } else {
                        input.setCustomValidity("");
                    }
                }
            });

            // 3. Opportunity Amount > 0
            var amountInput = form.querySelector("input[name='amount']");
            if (amountInput) {
                var amt = parseFloat(amountInput.value);
                if (isNaN(amt) || amt <= 0) {
                    amountInput.setCustomValidity("Opportunity Amount must be greater than 0.");
                    isValid = false;
                } else {
                    amountInput.setCustomValidity("");
                }
            }

            // 4. Probability 0 - 100
            var probInput = form.querySelector("input[name='probability']");
            if (probInput) {
                var prob = parseInt(probInput.value, 10);
                if (isNaN(prob) || prob < 0 || prob > 100) {
                    probInput.setCustomValidity("Probability must be between 0 and 100.");
                    isValid = false;
                } else {
                    probInput.setCustomValidity("");
                }
            }

            // 5. Follow-Up Date not earlier than today (for new/planned follow-ups)
            var fuDateInput = form.querySelector("input[name='followUpDate']");
            if (fuDateInput && fuDateInput.value) {
                var todayStr = new Date().toISOString().split("T")[0];
                if (fuDateInput.value < todayStr) {
                    fuDateInput.setCustomValidity("Follow-up date cannot be earlier than today.");
                    isValid = false;
                } else {
                    fuDateInput.setCustomValidity("");
                }
            }

            // 6. Expected Close Date not in past for active opportunities
            var closeDateInput = form.querySelector("input[name='expectedCloseDate']");
            var stageSelect = form.querySelector("select[name='stage']");
            if (closeDateInput && closeDateInput.value && stageSelect) {
                var stage = stageSelect.value;
                var todayStr = new Date().toISOString().split("T")[0];
                if (stage !== "WON" && stage !== "LOST" && closeDateInput.value < todayStr) {
                    closeDateInput.setCustomValidity("Expected Close Date cannot be in the past.");
                    isValid = false;
                } else {
                    closeDateInput.setCustomValidity("");
                }
            }

            if (!form.checkValidity() || !isValid) {
                event.preventDefault();
                event.stopPropagation();
            }

            form.classList.add("was-validated");
        }, false);
    });
});
