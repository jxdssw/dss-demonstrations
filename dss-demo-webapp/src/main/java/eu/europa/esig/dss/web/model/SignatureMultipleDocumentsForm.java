package eu.europa.esig.dss.web.model;

import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.utils.Utils;

public class SignatureMultipleDocumentsForm extends AbstractSignatureForm {

	@NotNull(message = "{error.container.type.mandatory}")
	private ASiCContainerType containerType;

	private List<MultipartFile> documentsToSign;

	public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

	public List<MultipartFile> getDocumentsToSign() {
		return documentsToSign;
	}

	public void setDocumentsToSign(List<MultipartFile> documentsToSign) {
		this.documentsToSign = documentsToSign;
	}

	@AssertTrue(message = "{error.to.sign.files.mandatory}")
	public boolean isDocumentsToSign() {
		if (Utils.isCollectionNotEmpty(documentsToSign)) {
			for (MultipartFile multipartFile : documentsToSign) {
				if (multipartFile != null && !multipartFile.isEmpty()) {
					// return true if at least one file is not empty
					return true;
				}
			}
		}
		return false;
	}

}
