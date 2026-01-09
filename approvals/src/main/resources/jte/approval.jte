@param no.sikt.nva.approvals.rest.ApprovalHtmlModel model

<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Approval - ${model.displayTitle()}</title>
  <style>
      body { margin: 0; padding: 0;
          background-color: #f1f1f1;
          font-family: "Roboto", "Helvetica", "Arial", sans-serif;
      }
      a, p { margin: 0; padding: 0; }
      h1, h2, h3 { margin: 0; padding: 0; }
      h1 {font-weight: 600; font-size: 1.3em; }
      h2 {font-weight: 600; font-size: 1.3em; }
      h3 {font-weight: 400; font-size: 1em; }
      .site-header {
          background-color: #0a0132;
          display: flex;
          justify-content: flex-start;
          align-items: center;
          height: 70px;
      }
      #home-button {
          color: white;
          font-size: 1em;
          text-decoration: none;
          width: 110px;
          margin-left: 20px;
      }
      .main {
          display: flex;
          justify-content: center;
      }
      .content {
          width: 900px;
          background-color: #ffffff;
          height: 500px;
          margin-top: 20px;
      }
      .header {
          background-color: #0a0132;
          display: flex;
          flex-direction: column;
          justify-content: center;
          height: 87px;
          border-left: 24px solid #dadfbf;
          color: #ffffff;;
          padding-left: 18px;
          h1 { margin-top: 10px;}
      }
      .content-body { padding: 20px; }
      .metadata {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          grid-template-rows: repeat(2, auto);
          gap: 1rem;
      }
      .metadata-label {
          font-size: 0.8em;
      }
      .metadata-text {
          font-size: 1em;
      }
      .member-field {
          margin-top: 10px;
      }
  </style>
</head>
<body>
  <div class="site-header"><a id="home-button" href="https://nva.sikt.no/">Nasjonalt vitenarkiv</a></div>
  <div class="main">
    <div class="content">
      <div class="header">
        <h3>Approval</h3>
        <h1>${model.displayTitle()}</h1>
      </div>
      <div class="content-body">
        <div class="metadata">
          @if(model.hasStudyPeriod())
          <div class="metadata-field">
            <p class="metadata-label">STUDY PERIOD</p>
            <p class="metadata-text">${model.studyPeriodStart()} - ${model.studyPeriodEnd()}</p>
          </div>
          @endif
          @for(var namedIdentifier : model.namedIdentifiers().entrySet())
          <div class="metadata-field">
            <p class="metadata-label">${namedIdentifier.getKey()}</p>
            <p class="metadata-text">${namedIdentifier.getValue()}</p>
          </div>
          @endfor
          <div class="metadata-field">
            <p class="metadata-label">HANDLE</p>
            <p class="metadata-text">${model.handle()}</p>
          </div>
          @for(var sponsor : model.sponsors())
          <div class="metadata-field">
            <p class="metadata-label">SPONSOR</p>
            <p class="metadata-text">${sponsor.name()}</p>
          </div>
          @endfor
        </div>
        @if(!model.trialSites().isEmpty())
        <hr>
        <h2>Members</h2>
        @for(var site : model.trialSites())
        <div class="member-field">
          @if(site.investigator().nvaPersonId() != null)
          <a href="${site.investigator().nvaPersonId()}">${site.investigator().title()} ${site.investigator().firstname()} ${site.investigator().lastname()}</a>
          @else
          <span>${site.investigator().title()} ${site.investigator().firstname()} ${site.investigator().lastname()}</span>
          @endif
          <p>${site.departmentName()}, ${site.investigator().department()}</p>
        </div>
        @endfor
        @endif
      </div>
    </div>
  </div>
</body>
</html>
