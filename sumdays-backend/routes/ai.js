const express = require('express');
const router = express.Router();

const mergeController = require('../controllers/ai/mergeController');
const analyzeController = require('../controllers/ai/analyzeController');
const ocrController = require('../controllers/ai/ocrController');
const sttController = require('../controllers/ai/sttController');
const extractController = require('../controllers/ai/extractController');

// api list
router.post('/merge', mergeController.merge); // merge two memos
// router.post('/merge-batch', mergeController.mergeBatch); // merge whole memos

router.post('/analyze', analyzeController.analyze); // analyze a diary: summary, emotion-score, emoji, feedback
router.post('/summarize-week', analyzeController.summarizeWeek); // summarize week
router.post('/summarize-month', analyzeController.summarizeMonth); // summarize month

router.post('/ocr/memo', ocrController.memo); // image memo to text
router.post('/ocr/diary', ocrController.diary); // image diary to text(must extract date)
router.post('/stt/memo', sttController.memo); // voice memo to text

router.post('/extract-style', extractController.extractStyle); // extract diary style from image/db diaries

module.exports = router;