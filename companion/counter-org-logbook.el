;;; counter-org-logbook.el --- Compute habit stats from org LOGBOOK entries  -*- lexical-binding: t; -*-

;;; Commentary:
;; Companion to Fylgja Android app. Reads CLOCK entries in
;; LOGBOOK drawers and computes streaks / today counts.

;;; Code:

(require 'org)
(require 'cl-lib)

(defvar logbook/habits-file "~/habits.org"
  "Path to the habits org file synced from Fylgja Android app.")

(defun logbook/parse-clock-entries ()
  "Parse CLOCK entries from current org buffer. Returns list of (date habit-name)."
  (cl-loop for entry in (logbook/find-logbook-drawers)
           collect (list (logbook/entry-date entry) entry)))

(defun logbook/find-logbook-drawers ()
  "Find all LOGBOOK drawers entries in current buffer."
  (let ((entries '()))
    (org-map-entries
     (lambda ()
       (let ((drawer (org-element-at-point)))
         (save-excursion
           (org-back-to-heading t)
           (let ((habit-name (substring-no-properties (org-get-heading t t))))
             (org-element-map (org-element-property :drawer (org-element-at-point))
                 'drawer
               (lambda (d)
                 (when (equal (org-element-property :drawer-name d) "LOGBOOK")
                   (org-element-map d 'clock
                     (lambda (c)
                       (push (list (org-timestamp-format (org-element-property :value c) "%Y-%m-%d")
                                   habit-name)
                             entries))))))))))
     t)
    entries))

(defun logbook/today-count (&optional habit-name)
  "Count CLOCK entries today, optionally filtered by HABIT-NAME."
  (with-current-buffer (find-file-noselect logbook/habits-file)
    (let ((today (format-time-string "%Y-%m-%d"))
          (count 0))
      (save-excursion
        (goto-char (point-min))
        (while (re-search-forward "^CLOCK: \\[\\([^]]+\\)\\]" nil t)
          (let* ((ts (match-string 1))
                 (date (substring ts 0 10))
                 (at-habit (save-excursion
                             (org-back-to-heading t)
                             (substring-no-properties (org-get-heading t t)))))
            (when (and (equal date today)
                       (or (null habit-name) (equal at-habit habit-name)))
              (cl-incf count)))))
      count)))

(defun logbook/streak-days (&optional habit-name)
  "Return number of consecutive days with at least one CLOCK entry."
  (with-current-buffer (find-file-noselect logbook/habits-file)
    (let ((dates (make-hash-table :test 'equal))
          (streak 0))
      (save-excursion
        (goto-char (point-min))
        (while (re-search-forward "^CLOCK: \\[\\([^]]+\\)\\]" nil t)
          (let* ((ts (match-string 1))
                 (date (substring ts 0 10))
                 (at-habit (save-excursion
                             (org-back-to-heading t)
                             (substring-no-properties (org-get-heading t t)))))
            (when (or (null habit-name) (equal at-habit habit-name))
              (puthash date t dates)))))
      (let ((d (days-to-time (time-to-days (current-time)))))
        (while (gethash (format-time-string "%Y-%m-%d" d) dates)
          (cl-incf streak)
          (setq d (time-subtract d 86400))))
      streak)))

(defun logbook/summary ()
  "Print habit summary to *Habit Logbook*."
  (interactive)
  (with-current-buffer (find-file-noselect logbook/habits-file)
    (let* ((buf (get-buffer-create "*Habit Logbook*"))
           (habits (org-element-map (org-element-parse-buffer) 'headline
                      (lambda (hl)
                        (when (equal (org-element-property :level hl) 2)
                          (substring-no-properties (org-element-property :raw-value hl)))))))
      (with-current-buffer buf
        (erase-buffer)
        (dolist (h habits)
          (insert (format "%s\t today=%d  streak=%d\n"
                          h
                          (logbook/today-count h)
                          (logbook/streak-days h))))
        (pop-to-buffer buf)))))

(provide 'counter-org-logbook)
;;; counter-org-logbook.el ends here
