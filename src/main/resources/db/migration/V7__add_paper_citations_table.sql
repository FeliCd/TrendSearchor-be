CREATE TABLE paper_citations (
    citing_paper_id BIGINT NOT NULL,
    cited_paper_id BIGINT NOT NULL,
    PRIMARY KEY (citing_paper_id, cited_paper_id),
    FOREIGN KEY (citing_paper_id) REFERENCES research_papers(id),
    FOREIGN KEY (cited_paper_id) REFERENCES research_papers(id)
);
