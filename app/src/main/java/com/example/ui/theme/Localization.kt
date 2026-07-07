package com.example.ui.theme

object Localization {
    private val en = mapOf(
        "app_name" to "SplitEase",
        "home" to "Home",
        "add_expense" to "Add Expense",
        "reports" to "Reports",
        "groups" to "Groups",
        "settings" to "Settings",
        "balance" to "Balance Summary",
        "who_owes_whom" to "Smart Settlements",
        "no_expenses_yet" to "No expenses recorded yet.",
        "settle_up" to "Settle Up",
        "activity_history" to "Activity History",
        "group_members" to "Group Members",
        "add_member" to "Add Member",
        "member_name" to "Member Name",
        "create_group" to "Create Group",
        "group_name" to "Group Name",
        "save" to "Save",
        "cancel" to "Cancel",
        "edit" to "Edit",
        "delete" to "Delete",
        "title" to "Title",
        "amount" to "Amount",
        "category" to "Category",
        "paid_by" to "Paid by",
        "split_type" to "How to Split",
        "equally" to "Equally among all",
        "by_percentage" to "By Percentage",
        "by_exact_amount" to "By Custom Exact",
        "recurring" to "Recurring Bill (Monthly)",
        "reminder_before_due" to "Bill Reminders",
        "category_food" to "Food",
        "category_rent" to "Rent",
        "category_utilities" to "Utilities",
        "category_entertainment" to "Entertainment",
        "category_shopping" to "Shopping",
        "category_other" to "Other",
        "total_spending" to "Total Spending",
        "spending_by_category" to "Category Breakdown",
        "spending_by_member" to "Member Spending",
        "owes" to "owes",
        "settled" to "settled",
        "delete_confirm" to "Are you sure you want to delete this?",
        "switch_group" to "Switch Group",
        "active_group" to "Active Group",
        "no_groups_yet" to "No groups found. Create a group to get started!",
        "select_payer" to "Select payer",
        "invalid_amount" to "Please enter a valid amount",
        "fill_required" to "Please fill in all required fields",
        "sum_not_match" to "Splits sum does not equal the total amount",
        "percentage_sum_error" to "Percentages must sum to 100%",
        "settle_success" to "Settlement recorded successfully",
        "add_expense_success" to "Expense added successfully",
        "edit_expense_success" to "Expense updated successfully",
        "group_created" to "Group created successfully",
        "member_added" to "Member added successfully",
        "theme" to "Theme",
        "light_mode" to "Light Mode",
        "dark_mode" to "Dark Mode",
        "language" to "Language",
        "currency" to "Currency Unit",
        "all_members" to "All Members",
        "due_soon" to "Due Soon",
        "no_settlements_yet" to "No settlements recorded yet.",
        "settled_up_message" to "All settled up! No debts in this group.",
        "total_borrowed" to "You owe",
        "total_lent" to "You are owed",
        "edit_expense" to "Edit Expense",
        "add_new_expense" to "Add New Expense",
        "payer" to "Payer",
        "percentage" to "Percentage",
        "exact_amount" to "Exact Amount",
        "outing_date" to "Trip / Outing Date",
        "ongoing" to "Ongoing",
        "finished" to "Finished",
        "status" to "Status",
        "mark_finished" to "Mark as Finished (Archive)",
        "mark_ongoing" to "Reopen Group (Ongoing)",
        "history_archive" to "Archived History",
        "active_groups" to "Active Groups",
        "not_set" to "Not set",
        "set_date" to "Set Date",
        "select_date" to "Select Date"
    )

    private val fa = mapOf(
        "app_name" to "اسپلیت‌ایز",
        "home" to "خانه",
        "add_expense" to "ثبت هزینه",
        "reports" to "گزارش‌ها",
        "groups" to "گروه‌ها",
        "settings" to "تنظیمات",
        "balance" to "خلاصه وضعیت مالی",
        "who_owes_whom" to "تسویه‌های هوشمند",
        "no_expenses_yet" to "هنوز هیچ هزینه‌ای ثبت نشده است.",
        "settle_up" to "تسویه حساب",
        "activity_history" to "تاریخچه فعالیت‌ها",
        "group_members" to "اعضای گروه",
        "add_member" to "افزودن عضو",
        "member_name" to "نام عضو",
        "create_group" to "ایجاد گروه جدید",
        "group_name" to "نام گروه",
        "save" to "ذخیره",
        "cancel" to "لغو",
        "edit" to "ویرایش",
        "delete" to "حذف",
        "title" to "عنوان",
        "amount" to "مبلغ",
        "category" to "دسته‌بندی",
        "paid_by" to "پرداخت‌کننده",
        "split_type" to "نحوه تقسیم",
        "equally" to "مساوی بین همه اعضا",
        "by_percentage" to "بر اساس درصد",
        "by_exact_amount" to "مبالغ سفارشی دقیق",
        "recurring" to "هزینه تکرارشونده (ماهانه)",
        "reminder_before_due" to "یادآور قبوض و بدهی‌ها",
        "category_food" to "غذا و رستوران",
        "category_rent" to "اجاره‌بها",
        "category_utilities" to "قبوض خدماتی",
        "category_entertainment" to "تفریح و سرگرمی",
        "category_shopping" to "خرید و پوشاک",
        "category_other" to "سایر هزینه‌ها",
        "total_spending" to "مجموع کل مخارج",
        "spending_by_category" to "هزینه‌ها به تفکیک دسته‌بندی",
        "spending_by_member" to "سهم هزینه هر عضو",
        "owes" to "بدهکار است به",
        "settled" to "تسویه حساب کرد با",
        "delete_confirm" to "آیا از حذف این مورد اطمینان دارید؟",
        "switch_group" to "تغییر گروه",
        "active_group" to "گروه فعال",
        "no_groups_yet" to "هیچ گروهی یافت نشد. برای شروع یک گروه ایجاد کنید!",
        "select_payer" to "انتخاب پرداخت‌کننده",
        "invalid_amount" to "لطفاً مبلغ معتبری وارد کنید",
        "fill_required" to "لطفاً تمامی فیلدهای الزامی را پر کنید",
        "sum_not_match" to "مجموع سهم‌ها با مبلغ کل هزینه برابر نیست",
        "percentage_sum_error" to "مجموع درصدها باید ۱۰۰٪ باشد",
        "settle_success" to "تسویه حساب با موفقیت ثبت شد",
        "add_expense_success" to "هزینه با موفقیت اضافه شد",
        "edit_expense_success" to "هزینه با موفقیت بروزرسانی شد",
        "group_created" to "گروه با موفقیت ایجاد شد",
        "member_added" to "عضو جدید با موفقیت اضافه شد",
        "theme" to "پوسته برنامه",
        "light_mode" to "حالت روشن",
        "dark_mode" to "حالت تاریک",
        "language" to "زبان برنامه",
        "currency" to "واحد پول پیش‌فرض",
        "all_members" to "همه اعضا",
        "due_soon" to "موعد نزدیک",
        "no_settlements_yet" to "هنوز تسویه‌ای ثبت نشده است.",
        "settled_up_message" to "همگی تسویه شده‌اند! هیچ بدهی در این گروه نیست.",
        "total_borrowed" to "بدهی کل شما",
        "total_lent" to "طلب کل شما",
        "edit_expense" to "ویرایش هزینه",
        "add_new_expense" to "ثبت هزینه جدید",
        "payer" to "پرداخت‌کننده",
        "percentage" to "درصد",
        "exact_amount" to "مبلغ دقیق",
        "outing_date" to "تاریخ سفر / بیرون رفتن",
        "ongoing" to "در جریان",
        "finished" to "پایان یافته / تسویه شده",
        "status" to "وضعیت",
        "mark_finished" to "بایگانی و خاتمه محاسبه",
        "mark_ongoing" to "بازگشایی مجدد گروه",
        "history_archive" to "بایگانی و تاریخچه",
        "active_groups" to "گروه‌های فعال",
        "not_set" to "تعیین نشده",
        "set_date" to "تعیین تاریخ",
        "select_date" to "انتخاب تاریخ"
    )

    fun getString(key: String, isFarsi: Boolean): String {
        return if (isFarsi) {
            fa[key] ?: en[key] ?: key
        } else {
            en[key] ?: key
        }
    }

    fun formatNumber(value: Double, isFarsi: Boolean, showDecimals: Boolean = false): String {
        val formatStr = if (showDecimals) {
            String.format("%.2f", value)
        } else {
            String.format("%.0f", value)
        }
        
        // Add commas to formatted number
        val parts = formatStr.split(".")
        val integerPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
        val finalStr = if (parts.size > 1 && showDecimals) "$integerPart.${parts[1]}" else integerPart

        return if (isFarsi) {
            formatPersianDigits(finalStr)
        } else {
            finalStr
        }
    }

    fun formatPersianDigits(text: String): String {
        val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return text.map { char ->
            if (char.isDigit()) {
                persianDigits[char.toString().toInt()]
            } else if (char == '.') {
                '٫'
            } else {
                char
            }
        }.joinToString("")
    }

    fun formatCurrency(value: Double, isFarsi: Boolean, customCurrency: String? = null): String {
        val formattedNum = formatNumber(value, isFarsi, showDecimals = !isFarsi)
        val defaultCurrency = if (isFarsi) "تومان" else "$"
        val unit = customCurrency ?: defaultCurrency
        
        return if (isFarsi) {
            "$formattedNum $unit"
        } else {
            "$unit$formattedNum"
        }
    }
}
