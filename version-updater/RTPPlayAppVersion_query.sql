INSERT INTO dbo.RTPPlayAppVersion  (CurrentVersion)
VALUES ('1.0.0');

SELECT * FROM dbo.RTPPlayAppVersion;

UPDATE dbo.RTPPlayAppVersion
SET CurrentVersion = '1.1'
WHERE id = '2D78DF3C-1BDF-4351-902D-B3A2F7355C44';